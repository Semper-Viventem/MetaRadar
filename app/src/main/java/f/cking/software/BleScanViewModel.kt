package f.cking.software

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class BleScanViewModel(
    private val permissionHelper: PermissionHelper,
) : ViewModel() {

    private val TAG = "BleScanViewModel"

    private lateinit var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val devices = mutableSetOf<BleDevice>()

    var devicesViewState by mutableStateOf(devices.toList())
    var scanStarted by mutableStateOf(false)

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = BleDevice(
                address = result.device.address,
                name = result.device.name,
                bondState = result.device.bondState,
                state = null,
                device = result.device,
            )

            devices.add(device)
            updateUiList(devices.toList())
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(TheApp.instance, "Scan failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Scan failed with error: $errorCode")
            cancelScanning()
        }
    }

    init {
        val bluetoothAdapter = TheApp.instance.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private fun updateUiList(devices: List<BleDevice>) {
        devicesViewState = devices.sortedBy { it.name }.reversed()
    }

    fun onActivityAttached() {
        scan()
    }

    fun onScanButtonClick() {
        scan()
    }

    fun onPermissionResult() {
        scan()
    }

    @SuppressLint("MissingPermission")
    private fun scan() {
        permissionHelper.checkBlePermissions {
            if (!scanStarted) {
                handler.postDelayed(::cancelScanning, 10_000L)
                scanStarted = true
                bluetoothScanner.startScan(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning() {
        scanStarted = false
        bluetoothScanner.stopScan(callback)
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                BleScanViewModel(TheApp.instance.permissionHelper)
            }
        }
    }
}