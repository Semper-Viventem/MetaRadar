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
    private val devicesRepository: DevicesRepository,
) : ViewModel() {

    private val TAG = "BleScanViewModel"

    private var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = mutableSetOf<BleDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()

    var devicesViewState by mutableStateOf(emptyList<DevicesRepository.DeviceData>())
    var scanStarted by mutableStateOf(false)

    private val generalComparator = Comparator<DevicesRepository.DeviceData> { first, second ->
        when {
            first.detectCount != second.detectCount -> first.detectCount.compareTo(second.detectCount)
            first.name != null && second.name != null -> first.name.compareTo(second.name)
            first.name != null && second.name == null -> 1
            first.name == null && second.name != null -> -1
            else -> first.address.compareTo(second.address)
        }
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
            Toast.makeText(TheApp.instance, "Scan failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Scan failed with error: $errorCode")
            cancelScanning()
        }
    }

    init {
        val bluetoothAdapter = TheApp.instance.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    }

    private fun updateUiList() {
        devicesViewState = devicesRepository.getDevices().sortedWith(generalComparator).reversed()
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
                batch.clear()
                handler.postDelayed(::cancelScanning, 10_000L)
                scanStarted = true
                currentScanTimeMs = System.currentTimeMillis()
                bluetoothScanner.startScan(callback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning() {
        scanStarted = false
        bluetoothScanner.stopScan(callback)
        devicesRepository.detectBatch(batch.toList())
        updateUiList()
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                BleScanViewModel(TheApp.instance.permissionHelper, TheApp.instance.devicesRepository)
            }
        }
    }
}