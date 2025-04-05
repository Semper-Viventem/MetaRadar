package f.cking.software.domain.interactor

import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.mapParallel
import f.cking.software.splitToBatchesEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DeviceServicesFetchingPlanner(
    private val fetchDeviceServiceInfo: FetchDeviceServiceInfo,
    private val bleScannerHelper: BleScannerHelper,
    private val saveReportInteractor: SaveReportInteractor,
) {
    private var parallelProcessingBatches = PARALLEL_BATCH_COUNT
    private var cooldownStartedAt: Long? = null
    private var lastJournalReportTime: Long = 0

    suspend fun scheduleFetchServiceInfo(devices: List<SavedDeviceHandle>): List<SavedDeviceHandle> =
        coroutineScope {
            val cooldown = this@DeviceServicesFetchingPlanner.cooldownStartedAt
            if (cooldown != null && System.currentTimeMillis() - cooldown < MIN_COOLDOWN_DURATION_MINS.minutes.inWholeMilliseconds) {
                Timber.tag(TAG).i("Device services fetching is on cooldown due to a high errors rate, current batch will be skipped")
                return@coroutineScope devices
            }

            val result =
                devices
                    .map { it }
                    .associateBy { it.device.address }
                    .toMutableMap()

            var updatedCount = 0
            var errors = 0
            var timeouts = 0

            withContext(Dispatchers.IO) {
                val metadataNeeded =
                    devices
                        .filter { checkIfMetadataUpdateNeeded(it) }
                        .map { it.device }
                        .sortedBy { it.rssi }
                        .reversed()

                Timber.tag(TAG).i("Scheduling fetch service info for ${metadataNeeded.size} devices, out of ${devices.size} total")
                val updated = fetchAllDevices(metadataNeeded)
                updated.forEach { fetchFeedback ->
                    when (fetchFeedback.feedback) {
                        FetchFeedback.SUCCESS -> {
                            fetchFeedback.device?.let { device ->
                                result[device.address]?.let { previousHandle ->
                                    result[device.address] = previousHandle.copy(device = device)
                                }
                                updatedCount++
                            }
                        }

                        FetchFeedback.TIMEOUT -> {
                            timeouts++
                        }

                        FetchFeedback.ERROR -> {
                            errors++
                        }
                    }
                }

                analyzeFeedback(metadataNeeded.size, updatedCount, timeouts, errors, devices.size)
                result.values.toList()
            }
        }

    private suspend fun fetchAllDevices(metadataNeeded: List<DeviceData>): List<DeviceFetchFeedback> =
        coroutineScope {
            val result = mutableListOf<DeviceFetchFeedback>()

            softTimeout(TOTAL_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
                Timber.tag(TAG).e("Timeout fetching total devices")
            }) {
                metadataNeeded
                    .splitToBatchesEqual(parallelProcessingBatches)
                    .filter { it.isNotEmpty() }
                    .mapParallel { batch ->
                        Timber.tag(TAG).i("Processing batch of ${batch.size} devices ($parallelProcessingBatches parallel)")
                        batch.map { device ->
                            result += fetchDevice(device)
                        }
                    }
            }

            result
        }

    private suspend fun fetchDevice(device: DeviceData): DeviceFetchFeedback =
        softTimeout(DEVICE_FETCH_TIMEOUT_SEC.seconds, onTimeout = {
            Timber.tag(TAG).e("Timeout fetching device info for ${device.address}")
            DeviceFetchFeedback(null, FetchFeedback.TIMEOUT)
        }) {
            Timber.tag(TAG).i("Fetching device info for ${device.address}, distance: ${device.distance()}")
            try {
                val result = fetchDeviceServiceInfo.execute(device)
                Timber.tag(TAG).i("Fetching complete for ${device.address}. Result: $result")
                DeviceFetchFeedback(result?.let { device.copy(metadata = it) }, FetchFeedback.SUCCESS)
            } catch (e: FetchDeviceServiceInfo.BluetoothConnectionException) {
                Timber.tag(TAG).e(e, "Error when connecting to device ${device.address}")
                DeviceFetchFeedback(null, FetchFeedback.ERROR)
            }
        }

    private data class DeviceFetchFeedback(
        val device: DeviceData?,
        val feedback: FetchFeedback,
    )

    private enum class FetchFeedback {
        TIMEOUT,
        ERROR,
        SUCCESS,
    }

    private suspend fun analyzeFeedback(
        updateNeeded: Int,
        updated: Int,
        timeouts: Int,
        errors: Int,
        total: Int,
    ) {
        Timber
            .tag(TAG)
            .i(
                "Deep analysis finished. Candidates: $updateNeeded (updated: $updated, timeouts: $timeouts, errors: $errors), total $total devices",
            )
        val errorsRate: Float = errors / (errors + timeouts + updated).toFloat()
        if (updateNeeded > 5 && errorsRate > 0.75f) {
            Timber.tag(TAG).e("Too many errors during deep analysis. Will try to reset ble stack and remove all connections")

            reportJournalEntity(updateNeeded, updated, timeouts, errors)
            bleScannerHelper.hardCloseAllConnections(TAG)
            cooldownStartedAt = System.currentTimeMillis()
        }
    }

    private suspend fun reportJournalEntity(
        updateNeeded: Int,
        updated: Int,
        timeouts: Int,
        errors: Int,
    ) {
        if (System.currentTimeMillis() - lastJournalReportTime < JOURNAL_REPORT_COOLDOWN_MIN.minutes.inWholeMilliseconds) {
            return
        }
        val report =
            JournalEntry.Report.Error(
                title = "Too many errors during deep analysis. Restart bluetooth or disable deep analysis in settings",
                stackTrace = "Errors: $errors, timeouts: $timeouts, updated: $updated, in total: $updateNeeded",
            )
        saveReportInteractor.execute(report)
        lastJournalReportTime = System.currentTimeMillis()
    }

    private suspend fun <T> softTimeout(
        timeout: Duration,
        onTimeout: suspend () -> T,
        block: suspend () -> T,
    ): T =
        coroutineScope {
            channelFlow<T> {
                var timeoutJob: Job? = null
                val primaryJob =
                    async {
                        val result = block.invoke()
                        timeoutJob?.cancel()
                        send(result)
                    }

                timeoutJob =
                    async {
                        delay(timeout)
                        primaryJob.cancel()
                        send(onTimeout.invoke())
                    }
            }.first()
        }

    private fun checkIfMetadataUpdateNeeded(device: SavedDeviceHandle): Boolean {
        if (!device.device.isConnectable) {
            return false
        }

        val lastSeenSecAgo = (System.currentTimeMillis() - device.previouslySeenAtTime)
        val recentlyChecked = lastSeenSecAgo < CHECK_INTERVAL_PER_DEVICE_MIN.minutes.inWholeMilliseconds

        val isNewDevice = device.device.detectCount == 1

        return isNewDevice ||
            device.device.metadata.isNullOrEmpty() ||
            !recentlyChecked
    }

    private fun decreaseMaxConnections() {
        parallelProcessingBatches = max(MIN_PARALLEL_CONNECTIONS, parallelProcessingBatches - 1)
    }

    private fun increaseConnections() {
        parallelProcessingBatches = min(parallelProcessingBatches + 1, MAX_PARALLEL_CONNECTIONS)
    }

    companion object {
        private const val PARALLEL_BATCH_COUNT = 7 // usually 7 parallel connections are stable
        private const val MIN_PARALLEL_CONNECTIONS = 2
        private const val MAX_PARALLEL_CONNECTIONS = 15
        private const val CHECK_INTERVAL_PER_DEVICE_MIN = 10
        private const val JOURNAL_REPORT_COOLDOWN_MIN = 30
        private const val DEVICE_FETCH_TIMEOUT_SEC = 8
        private const val TOTAL_FETCH_TIMEOUT_SEC = 30
        private const val MIN_COOLDOWN_DURATION_MINS = 1
        private const val TAG = "DeviceServicesFetchingPlanner"
    }
}
