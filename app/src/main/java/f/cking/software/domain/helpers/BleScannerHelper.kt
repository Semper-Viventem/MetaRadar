package f.cking.software.domain.helpers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import f.cking.software.domain.interactor.GetKnownDevicesInteractor
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.*

class BleScannerHelper(
    private val devicesRepository: DevicesRepository,
    private val getKnownDevicesInteractor: GetKnownDevicesInteractor,
    appContext: Context,
) {

    private val TAG = "BleScannerHelper"

    private var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = hashMapOf<String, BleScanDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()
    private val previouslyNoticedServicesUUIDs = mutableSetOf<String>()

    var inProgress = MutableStateFlow(false)

    private var scanListener: ScanListener? = null

    init {
        val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.serviceUuids?.map { previouslyNoticedServicesUUIDs.add(it.uuid.toString()) }
            val device = BleScanDevice(
                address = result.device.address,
                name = result.device.name,
                scanTimeMs = currentScanTimeMs,
                scanRecordRaw = result.scanRecord?.bytes,
            )

            batch.put(device.address, device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan failed with error: $errorCode")
            cancelScanning(ScanResultInternal.FAILURE)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun scan(
        scanRestricted: Boolean = false,
        scanDurationMs: Long,
        scanListener: ScanListener,
    ) {
        Log.d(TAG, "Start BLE Scan. Restricted mode: $scanRestricted")

        if (inProgress.value) {
            Log.e(TAG, "BLE Scan failed because previous scan is not finished")
        } else {
            this@BleScannerHelper.scanListener = scanListener
            batch.clear()
            handler.postDelayed({ cancelScanning(ScanResultInternal.SUCCESS) }, scanDurationMs)
            inProgress.tryEmit(true)
            currentScanTimeMs = System.currentTimeMillis()

            val scanFilters = if (scanRestricted) {
                getBGFilters() + getPopularServiceUUIDS()
            } else {
                listOf(ScanFilter.Builder().build())
            }

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            withContext(Dispatchers.IO) {
                bluetoothScanner.startScan(scanFilters, scanSettings, callback)
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

    private suspend fun getBGFilters(): List<ScanFilter> {
        return getKnownDevicesInteractor.execute().map {
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
        inProgress.tryEmit(false)
        bluetoothScanner.stopScan(callback)


        when (scanResult) {
            ScanResultInternal.SUCCESS -> {
                Log.d(TAG, "BLE Scan finished ${batch.count()} devices found")
                scanListener?.onSuccess(batch.values.toList())
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

    private enum class ScanResultInternal { SUCCESS, FAILURE, CANCELED }

    companion object {
        private val popularServicesUUID = setOf(
            "0000fe8f-0000-1000-8000-00805f9b34fb",
            "0000fe9f-0000-1000-8000-00805f9b34fb",
            "0000feb8-0000-1000-8000-00805f9b34fb",
            "0000fd5a-0000-1000-8000-00805f9b34fb",
        )
    }
}