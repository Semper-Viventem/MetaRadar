package f.cking.software.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.TheApp
import f.cking.software.domain.BleScannerHelper
import f.cking.software.domain.DeviceData
import f.cking.software.domain.DevicesRepository
import f.cking.software.domain.PermissionHelper
import f.cking.software.service.BgScanWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BleScanViewModel(
    private val permissionHelper: PermissionHelper,
    private val devicesRepository: DevicesRepository,
    private val bleScanner: BleScannerHelper,
) : ViewModel() {

    private val TAG = "BleScanViewModel"

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())
    var scanStarted by mutableStateOf(false)

    private val updateListHandler: Handler = Handler(Looper.getMainLooper())
    private val updateListRunnable: Runnable = Runnable {
        updateUiList()
        rescheduleListUpdate()
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

    private fun updateUiList() {
        viewModelScope.launch(Dispatchers.IO) {
            val devices = devicesRepository.getDevices().sortedWith(generalComparator).reversed()

            launch(Dispatchers.Main) {
                devicesViewState = devices
            }
        }
    }

    private fun rescheduleListUpdate() {
        updateListHandler.postDelayed(updateListRunnable, LIST_UPDATE_TIME_MS)
    }

    fun onActivityAttached() {
        updateUiList()
        rescheduleListUpdate()
    }

    fun onScanButtonClick() {
        scan()
    }

    fun onPermissionResult() {
        scan()
    }

    private fun scan() {
        permissionHelper.checkBlePermissions {
            if (!scanStarted) {
                scanStarted = true
                bleScanner.scan { batch ->
                    scanStarted = false
                    viewModelScope.launch(Dispatchers.IO) {
                        devicesRepository.detectBatch(batch.toList())
                        updateUiList()
                    }
                }
            }
        }
    }

    fun runBackgroundScanning() {
        permissionHelper.checkBlePermissions(
            permissionRequestCode = PermissionHelper.PERMISSIONS_BACKGROUND_REQUEST_CODE,
            permissions = PermissionHelper.BACKGROUND_LOCATION
        ) {
            BgScanWorker.schedule(TheApp.instance)
        }
    }

    companion object {
        private const val LIST_UPDATE_TIME_MS = 15_000L
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