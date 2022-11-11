package f.cking.software.domain.helpers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class BleScannerHelper(
    private val devicesRepository: DevicesRepository,
    appContext: Context,
) {

    private val TAG = "BleScannerHelper"

    private var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = mutableSetOf<BleScanDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()
    private val previouslyNoticedServicesUUIDs = mutableSetOf<String>()

    private var inProgress: Boolean = false
        set(value) {
            field = value
            progressListeners.forEach { it.onScanProgressChanged(field) }
        }

    private var scanListener: ScanListener? = null
    private var progressListeners: MutableSet<ProgressListener> = mutableSetOf()

    init {
        val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    }

    fun addProgressListener(listener: ProgressListener) {
        progressListeners.add(listener)
        listener.onScanProgressChanged(inProgress)
    }

    fun removeProgressListener(listener: ProgressListener) {
        progressListeners.remove(listener)
    }

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.serviceUuids?.map { previouslyNoticedServicesUUIDs.add(it.uuid.toString()) }
            val device = BleScanDevice(
                address = result.device.address,
                name = result.device.name,
                bondState = result.device.bondState,
                scanTimeMs = currentScanTimeMs,
            )

            batch.add(device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed with error: $errorCode")
            cancelScanning(ScanResultInternal.FAILURE)
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(
        scanRestricted: Boolean = false,
        scanDurationMs: Long = DEFAULT_SCAN_DURATION,
        scanListener: ScanListener,
    ) {
        runBlocking {
            launch(Dispatchers.IO) {
                Log.d(TAG, "Start BLE Scan. Restricted mode: $scanRestricted")

                if (inProgress) {
                    Log.e(TAG, "BLE Scan failed because previous scan is not finished")
                } else {
                    this@BleScannerHelper.scanListener = scanListener
                    batch.clear()
                    handler.postDelayed({ cancelScanning(ScanResultInternal.SUCCESS) }, scanDurationMs)
                    inProgress = true
                    currentScanTimeMs = System.currentTimeMillis()

                    val scanFilters = if (scanRestricted) {
                        getBGFilters() + getPopularServiceUUIDS()
                    } else {
                        listOf(ScanFilter.Builder().build())
                    }

                    val scanSettings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()

                    bluetoothScanner.startScan(scanFilters, scanSettings, callback)
                }
            }
        }
    }

    private fun getPopularServiceUUIDS(): List<ScanFilter> {
        return (popularServicesUUID + previouslyNoticedServicesUUIDs).map { uuid ->
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(uuid))
                .build()
        }
    }

    private fun getBGFilters(): List<ScanFilter> {
        return devicesRepository.getKnownDevices().map {
            ScanFilter.Builder()
                .setDeviceAddress(it.address)
                .build()
        }
    }

    fun stopScanning() {
        cancelScanning(ScanResultInternal.CANCELED)
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning(scanResult: ScanResultInternal) {
        inProgress = false
        bluetoothScanner.stopScan(callback)


        when (scanResult) {
            ScanResultInternal.SUCCESS -> {
                Log.d(TAG, "BLE Scan finished ${batch.count()} devices found")
                scanListener?.onSuccess(batch.toList())
            }
            ScanResultInternal.FAILURE -> {
                scanListener?.onFailure()
            }
            ScanResultInternal.CANCELED -> {
                // do nothing
            }
        }
        scanListener = null
    }

    interface ScanListener {
        fun onSuccess(batch: List<BleScanDevice>)
        fun onFailure()
    }

    interface ProgressListener {
        fun onScanProgressChanged(inProgress: Boolean)
    }

    private enum class ScanResultInternal { SUCCESS, FAILURE, CANCELED }

    companion object {
        const val DEFAULT_SCAN_DURATION = 10_000L // 10 sec
        private val popularServicesUUID = setOf(
            "0000fe8f-0000-1000-8000-00805f9b34fb",
            "0000fe9f-0000-1000-8000-00805f9b34fb",
            "0000feb8-0000-1000-8000-00805f9b34fb",
            "0000fd5a-0000-1000-8000-00805f9b34fb",
        )
    }
}