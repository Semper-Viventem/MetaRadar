package f.cking.software.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import f.cking.software.TheApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class BleScannerHelper(
    private val appContext: Context,
) {

    private val TAG = "BleScannerHelper"

    private var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = mutableSetOf<BleDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()
    private val previouslyNoticedServicesUUIDs = mutableSetOf<String>()

    private var inProgress: Boolean = false
        set(value) {
            field = value
            listeners.forEach { it.onScanProgressChanged(field) }
        }

    private var scanListener: ScanListener? = null
    private var listeners: MutableSet<Listener> = mutableSetOf()

    init {
        val bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.onScanProgressChanged(inProgress)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.serviceUuids?.map { previouslyNoticedServicesUUIDs.add(it.uuid.toString()) }
            val device = BleDevice(
                address = result.device.address,
                name = result.device.name,
                bondState = result.device.bondState,
                scanTimeMs = currentScanTimeMs,
            )

            batch.add(device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error: $errorCode")
            cancelScanning(ScanResultInternal.FAILURE)
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(
        scanRestricted: Boolean = false,
        scanListener: ScanListener,
    ) {
        runBlocking {
            launch(Dispatchers.IO) {
                this@BleScannerHelper.scanListener = scanListener

                if (inProgress) {
                    scanListener.onFailure()
                } else {
                    batch.clear()
                    handler.postDelayed({ cancelScanning(ScanResultInternal.SUCCESS) }, 10_000L)
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
        return TheApp.instance.devicesRepository.getKnownDevices().map {
            ScanFilter.Builder()
                .setDeviceAddress(it.address)
                .build()
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning(scanResult: ScanResultInternal) {
        inProgress = false
        bluetoothScanner.stopScan(callback)

        when (scanResult) {
            ScanResultInternal.SUCCESS -> scanListener?.onSuccess(batch.toList())
            ScanResultInternal.FAILURE -> scanListener?.onFailure()
        }
    }

    interface ScanListener {
        fun onSuccess(batch: List<BleDevice>)
        fun onFailure()
    }

    interface Listener {
        fun onScanProgressChanged(inProgress: Boolean)
    }

    private enum class ScanResultInternal { SUCCESS, FAILURE }

    companion object {
        private val popularServicesUUID = setOf(
            "0000fe8f-0000-1000-8000-00805f9b34fb",
            "0000fe9f-0000-1000-8000-00805f9b34fb",
            "0000feb8-0000-1000-8000-00805f9b34fb",
            "0000fd5a-0000-1000-8000-00805f9b34fb",
        )
    }
}