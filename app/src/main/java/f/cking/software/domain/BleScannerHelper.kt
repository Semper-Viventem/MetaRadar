package f.cking.software.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class BleScannerHelper(
    private val appContext: Context,
) {

    private val TAG = "BleScannerHelper"

    private var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = mutableSetOf<BleDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()

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
        scanListener: ScanListener,
    ) {
        this.scanListener = scanListener

        if (inProgress) {
            scanListener.onFailure()
            return
        }

        batch.clear()
        handler.postDelayed({ cancelScanning(ScanResultInternal.SUCCESS) }, 10_000L)
        inProgress = true
        currentScanTimeMs = System.currentTimeMillis()
        bluetoothScanner.startScan(callback)
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
}