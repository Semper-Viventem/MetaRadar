package f.cking.software.ui

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.TheApp
import f.cking.software.domain.*
import f.cking.software.service.BgScanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BleScanViewModel(
    private val permissionHelper: PermissionHelper,
    private val devicesRepository: DevicesRepository,
    private val bleScanner: BleScannerHelper,
) : ViewModel(), BleScannerHelper.Listener, DevicesRepository.OnDevicesUpdateListener {

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())
    var scanStarted by mutableStateOf(false)

    init {
        devicesRepository.addListener(this)
        bleScanner.addListener(this)
    }

    private val generalComparator = Comparator<DeviceData> { first, second ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)
            first.detectCount != second.detectCount -> first.detectCount.compareTo(second.detectCount)
            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1) ?: -1
            first.firstDetectTimeMs != second.firstDetectTimeMs -> second.firstDetectTimeMs.compareTo(first.firstDetectTimeMs)
            else -> first.address.compareTo(second.address)
        }
    }

    fun onScanButtonClick() {
        scan()
    }

    override fun onScanProgressChanged(inProgress: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            scanStarted = inProgress
        }
    }

    override fun onDevicesUpdate(devices: List<DeviceData>) {
        viewModelScope.launch(Dispatchers.Main) {
            devicesViewState = devices.sortedWith(generalComparator).reversed()
        }
    }

    private fun scan() {
        permissionHelper.checkBlePermissions {
            bleScanner.scan(object : BleScannerHelper.ScanListener {
                override fun onSuccess(batch: List<BleDevice>) {
                    viewModelScope.launch(Dispatchers.IO) {
                        devicesRepository.detectBatch(batch)
                    }
                }

                override fun onFailure() {
                    Toast.makeText(TheApp.instance, "Scan failed", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    fun runBackgroundScanning() {
        permissionHelper.checkBlePermissions(
            permissionRequestCode = PermissionHelper.PERMISSIONS_BACKGROUND_REQUEST_CODE,
            permissions = PermissionHelper.BACKGROUND_LOCATION
        ) {
            permissionHelper.checkDozeModePermission()
            if (TheApp.instance.activeWorkId.isPresent) {
                BgScanService.stop(TheApp.instance)
            } else {
                BgScanService.schedule(TheApp.instance)
            }
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                BleScanViewModel(
                    permissionHelper = TheApp.instance.permissionHelper,
                    devicesRepository = TheApp.instance.devicesRepository,
                    bleScanner = TheApp.instance.bleScannerHelper
                )
            }
        }
    }
}