package f.cking.software.ui.main

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.service.BgScanService
import f.cking.software.ui.devicelist.DeviceListScreen
import f.cking.software.ui.radarprofile.RadarProfileScreen
import f.cking.software.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class MainViewModel(
    private val permissionHelper: PermissionHelper,
    private val appContext: Application,
    private val bleScanner: BleScannerHelper,
) : ViewModel() {

    var scanStarted: Boolean by mutableStateOf(bleScanner.inProgress.value)
    var bgServiceIsActive: Boolean by mutableStateOf(BgScanService.isActive.value)

    var tabs by mutableStateOf(
        listOf(
            Tab(
                iconRes = R.drawable.ic_home_outline,
                selectedIconRes = R.drawable.ic_home,
                text = "Device list",
                selected = true
            ) { DeviceListScreen.Screen() },
            Tab(
                iconRes = R.drawable.ic_settings_outline,
                selectedIconRes = R.drawable.ic_settings,
                text = "Settings",
                selected = false
            ) { SettingsScreen.Screen() },
            Tab(
                iconRes = R.drawable.ic_search_outline,
                selectedIconRes = R.drawable.ic_search,
                text = "Radar profiles",
                selected = false
            ) { RadarProfileScreen.Screen() },
        )
    )

    init {
        observeScanInProgress()
        observeServiceIsLaunched()
    }

    fun onScanButtonClick() {
        checkPermissions {
            BgScanService.scan(appContext)
        }
    }

    fun onTabClick(tab: Tab) {
        val list = tabs.map { it.copy(selected = it == tab) }
        tabs = list
    }

    fun runBackgroundScanning() {
        checkPermissions {
            permissionHelper.checkDozeModePermission()
            if (BgScanService.isActive.value) {
                BgScanService.stop(appContext)
            } else {
                BgScanService.start(appContext)
            }
        }
    }

    private fun observeScanInProgress() {
        viewModelScope.launch {
            bleScanner.inProgress
                .collect { scanStarted = it }
        }
    }

    private fun observeServiceIsLaunched() {
        viewModelScope.launch {
            BgScanService.isActive
                .collect { bgServiceIsActive = it }
        }
    }

    private fun checkPermissions(granted: () -> Unit) {
        permissionHelper.checkBlePermissions {
            permissionHelper.checkBlePermissions(permissions = PermissionHelper.BACKGROUND_LOCATION) {
                permissionHelper.checkDozeModePermission()
                granted.invoke()
            }
        }
    }

    data class Tab(
        @DrawableRes val iconRes: Int,
        @DrawableRes val selectedIconRes: Int,
        val text: String,
        val selected: Boolean,
        val screen: @Composable () -> Unit,
    )
}