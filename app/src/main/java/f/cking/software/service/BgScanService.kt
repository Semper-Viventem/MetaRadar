package f.cking.software.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.NotificationsHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.AnalyseScanBatchInteractor
import f.cking.software.domain.interactor.CheckProfileDetectionInteractor
import f.cking.software.domain.interactor.SaveReportInteractor
import f.cking.software.domain.interactor.SaveScanBatchInteractor
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject


class BgScanService : Service() {

    private val TAG = "BgScanService"

    private val permissionHelper: PermissionHelper by inject()
    private val bleScannerHelper: BleScannerHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val locationProvider: LocationProvider by inject()
    private val notificationsHelper: NotificationsHelper by inject()

    private val saveScanBatchInteractor: SaveScanBatchInteractor by inject()
    private val analyseScanBatchInteractor: AnalyseScanBatchInteractor by inject()
    private val saveReportInteractor: SaveReportInteractor by inject()
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private val handler = Handler(Looper.getMainLooper())
    private var failureScanCounter: Int = 0
    private var locationDisabledWasReported: Boolean = false
    private var bluetoothDisabledWasReported: Boolean = false
    private val nextScanRunnable = Runnable {
        scan()
    }

    private val bleListener = object : BleScannerHelper.ScanListener {
        override fun onFailure(exception: Exception) {
            handleError(exception)
        }

        override fun onSuccess(batch: List<BleScanDevice>) {
            handleScanResult(batch)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        isActive.tryEmit(true)
    }

    private fun handleError(exception: Throwable) {
        failureScanCounter++

        reportError(exception)

        if (failureScanCounter >= MAX_FAILURE_SCANS_TO_CLOSE) {
            reportError(RuntimeException("Ble Scan service was stopped after $MAX_FAILURE_SCANS_TO_CLOSE errors"))
            stopSelf()
        } else {
            scheduleNextScan()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Background service close action handled")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (intent != null && intent.action == ACTION_SCAN_NOW) {
            Log.d(TAG, "Background service scan now command")
            scan()
        } else {
            Log.d(TAG, "Background service launched")
            startForeground(
                NotificationsHelper.FOREGROUND_NOTIFICATION_ID,
                notificationsHelper.buildForegroundNotification(
                    NotificationsHelper.ServiceNotificationContent.NoDataYet,
                    createCloseServiceIntent(this)
                )
            )

            permissionHelper.checkBlePermissions(
                onRequestPermissions = { _, _, _ ->
                    reportError(IllegalStateException("BLE Service is started but permissins are not granted"))
                    stopSelf()
                },
                onPermissionGranted =  {
                    locationProvider.startLocationFetching()
                    scan()
                }
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Background service destroyed")
        scope.cancel()
        isActive.tryEmit(false)
        bleScannerHelper.stopScanning()
        locationProvider.stopLocationListening()
        handler.removeCallbacks(nextScanRunnable)
        notificationsHelper.cancel(NotificationsHelper.FOREGROUND_NOTIFICATION_ID)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun scan() {
        scope.launch {
            try {
                bleScannerHelper.scan(
                    scanRestricted = isNonInteractiveMode(),
                    scanDurationMs = settingsRepository.getScanDuration(),
                    scanListener = bleListener,
                )
            } catch (e: BleScannerHelper.BluetoothIsNotInitialized) {
                handleBleIsTurnedOffError()
                notificationsHelper.updateNotification(
                    NotificationsHelper.ServiceNotificationContent.BluetoothIsTurnedOff,
                    createCloseServiceIntent(this@BgScanService)
                )
                scheduleNextScan()
            } catch (e: Throwable) {
                reportError(e)
                stopSelf()
            }
        }
    }

    private fun handleScanResult(batch: List<BleScanDevice>) {
        scope.launch {

            val notificationContent: NotificationsHelper.ServiceNotificationContent =
                if (batch.isEmpty() && !locationProvider.isLocationAvailable() && !locationDisabledWasReported) {
                    notificationsHelper.notifyLocationIsTurnedOff()
                    reportError(IllegalStateException("The BLE scanner did not return anything. This may happen if geolocation is turned off at the system level. Location access is required to work with BLE on Android."))
                    locationDisabledWasReported = true
                    NotificationsHelper.ServiceNotificationContent.LocationIsTurnedOff
                } else if (batch.isEmpty() && !bleScannerHelper.isBluetoothEnabled()) {
                    handleBleIsTurnedOffError()
                    NotificationsHelper.ServiceNotificationContent.BluetoothIsTurnedOff
                } else if (batch.isNotEmpty()) {
                    locationDisabledWasReported = false
                    bluetoothDisabledWasReported = false


                    try {
                        val analyseResult = analyseScanBatchInteractor.execute(batch)
                        withContext(Dispatchers.Default) {
                            saveScanBatchInteractor.execute(batch)
                        }

                        withContext(Dispatchers.Main) {
                            handleAnalysResult(analyseResult)
                        }

                        failureScanCounter = 0

                        if (analyseResult.knownDevicesCount > 0) {
                            NotificationsHelper.ServiceNotificationContent.KnownDevicesAround(analyseResult.knownDevicesCount)
                        } else {
                            NotificationsHelper.ServiceNotificationContent.TotalDevicesAround(batch.size)
                        }
                    } catch (exception: Throwable) {
                        handleError(exception)
                        NotificationsHelper.ServiceNotificationContent.NoDataYet
                    }
                } else {
                    NotificationsHelper.ServiceNotificationContent.NoDataYet
                }

            notificationsHelper.updateNotification(notificationContent, createCloseServiceIntent(this@BgScanService))

            scheduleNextScan()
        }
    }

    private fun handleBleIsTurnedOffError() {
        if (!bluetoothDisabledWasReported) {
            notificationsHelper.notifyBluetoothIsTurnedOff()
            reportError(BleScannerHelper.BluetoothIsNotInitialized())
            bluetoothDisabledWasReported = true
        }
    }

    /**
     * BLE scan is limited if device's screen is turned off
     */
    private fun isNonInteractiveMode(): Boolean {
        return !powerManager.isInteractive
    }

    private fun handleProfileCheckingResult(profiles: List<CheckProfileDetectionInteractor.ProfileResult>) {
        if (profiles.isNotEmpty()) {
            notificationsHelper.notifyRadarProfile(profiles)
        }
    }

    private fun handleAnalysResult(result: AnalyseScanBatchInteractor.Result) {
        Log.d(TAG, "Background scan result: known_devices_count=${result.knownDevicesCount}, matched_profiles=${result.matchedProfiles.count()}")
        handleProfileCheckingResult(result.matchedProfiles)
    }

    private fun scheduleNextScan() {
        val interval = if (isNonInteractiveMode()) {
            settingsRepository.getScanRestrictedInterval()
        } else {
            settingsRepository.getScanInterval()
        }
        handler.postDelayed(nextScanRunnable, interval)
    }

    private fun reportError(error: Throwable) {
        Log.e(TAG, error.message.orEmpty(), error)
        scope.launch {
            val report = JournalEntry.Report.Error(
                title = "[BLE Service Error]: ${error.message ?: error::class.java}",
                stackTrace = error.stackTraceToString(),
            )
            saveReportInteractor.execute(report)
        }
    }

    companion object {
        private const val MAX_FAILURE_SCANS_TO_CLOSE = 10

        private const val ACTION_STOP_SERVICE = "stop_ble_scan_service"
        private const val ACTION_SCAN_NOW = "ble_scan_now"

        var isActive = MutableStateFlow(false)

        private fun createCloseServiceIntent(context: Context): Intent {
            return Intent(context, BgScanService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, BgScanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            if (isActive.value) {
                context.startService(createCloseServiceIntent(context))
            }
        }

        fun scan(context: Context) {
            if (isActive.value) {
                val intent = Intent(context, BgScanService::class.java).apply {
                    action = ACTION_SCAN_NOW
                }
                context.startService(intent)
            } else {
                start(context)
            }
        }
    }
}