package f.cking.software.ui.main

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanpra.composematerialdialogs.MaterialDialogState
import f.cking.software.R
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.service.BgScanService
import f.cking.software.ui.devicelist.DeviceListScreen
import f.cking.software.ui.journal.JournalScreen
import f.cking.software.ui.profileslist.ProfilesListScreen
import f.cking.software.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class MainViewModel(
    private val permissionHelper: PermissionHelper,
    private val context: Application,
    private val bluetoothHelper: BleScannerHelper,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val intentHelper: IntentHelper,
) : ViewModel() {

    var scanStarted: Boolean by mutableStateOf(bluetoothHelper.inProgress.value)
    var bgServiceIsActive: Boolean by mutableStateOf(BgScanService.isActive.value)
    var showLocationDisabledDialog: MaterialDialogState = MaterialDialogState()
    var showBluetoothDisabledDialog: MaterialDialogState = MaterialDialogState()

    var tabs by mutableStateOf(
        listOf(
            Tab(
                iconRes = R.drawable.ic_home_outline,
                selectedIconRes = R.drawable.ic_home,
                text = context.getString(R.string.menu_device_list),
                selected = true,
            ) { DeviceListScreen.Screen() },
            Tab(
                iconRes = R.drawable.ic_search_outline,
                selectedIconRes = R.drawable.ic_search,
                text = context.getString(R.string.menu_radar_profiles),
                selected = false,
            ) { ProfilesListScreen.Screen() },
            Tab(
                iconRes = R.drawable.ic_journal_outline,
                selectedIconRes = R.drawable.ic_journal,
                text = context.getString(R.string.menu_journal),
                selected = false,
            ) { JournalScreen.Screen() },
            Tab(
                iconRes = R.drawable.ic_settings_outline,
                selectedIconRes = R.drawable.ic_settings,
                text = context.getString(R.string.menu_settings),
                selected = false,
            ) { SettingsScreen.Screen() },
        )
    )

    init {
        observeScanInProgress()
        observeServiceIsLaunched()
    }

    fun onScanButtonClick() {
        checkPermissions {
            BgScanService.scan(context)
        }
    }

    fun onTabClick(tab: Tab) {
        val list = tabs.map { it.copy(selected = it == tab) }
        tabs = list
    }

    fun runBackgroundScanning() {
        checkPermissions {
            if (BgScanService.isActive.value) {
                BgScanService.stop(context)
            } else if (!locationProvider.isLocationAvailable()) {
                showLocationDisabledDialog.show()
            } else if (!bluetoothHelper.isBluetoothEnabled()) {
                showBluetoothDisabledDialog.show()
            } else {
                BgScanService.start(context)
            }
        }
    }

    fun onTurnOnLocationClick() {
        intentHelper.openLocationSettings()
    }

    fun onTurnOnBluetoothClick() {
        intentHelper.openBluetoothSettings()
    }

    fun needToShowPermissionsIntro(): Boolean {
        return !settingsRepository.getPermissionsIntroWasShown()
    }

    fun userHasPassedPermissionsIntro() {
        settingsRepository.setPermissionsIntroWasShown(true)
    }

    private fun observeScanInProgress() {
        viewModelScope.launch {
            bluetoothHelper.inProgress
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