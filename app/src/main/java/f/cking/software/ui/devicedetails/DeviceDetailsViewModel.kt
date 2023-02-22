package f.cking.software.ui.devicedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.toDomain
import kotlinx.coroutines.launch

class DeviceDetailsViewModel(
    private val router: NavRouter,
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val permissionHelper: PermissionHelper,
) : ViewModel() {

    var deviceState: DeviceData? by mutableStateOf(null)
    var points: List<LocationModel> by mutableStateOf(emptyList())
    var historyPeriod: HistoryPeriod by mutableStateOf(HistoryPeriod.DAY)
    var currentLocation: LocationModel? by mutableStateOf(null)

    fun setAddress(address: String) {
        viewModelScope.launch {
            deviceState = null
            points = emptyList()
            historyPeriod = HistoryPeriod.DAY
            observeLocation()
            loadDevice(address)
        }
    }

    private suspend fun loadDevice(address: String) {
        val device = devicesRepository.getDeviceByAddress(address)
        if (device == null) {
            back()
        } else {
            deviceState = device
            refreshLocationHistory(address)
        }
    }

    private fun observeLocation() {
        permissionHelper.checkBlePermissions {
            viewModelScope.launch {
                locationProvider.observeLocation()
                    .collect { location ->
                        currentLocation = location?.location?.toDomain(System.currentTimeMillis())
                    }
            }
        }
    }

    private suspend fun refreshLocationHistory(address: String) {
        val fromTime = System.currentTimeMillis() - historyPeriod.periodMills
        points = locationRepository.getAllLocationsByAddress(address, fromTime = fromTime)
    }

    fun selectHistoryPeriodSelected(newHistoryPeriod: HistoryPeriod, device: DeviceData) {
        viewModelScope.launch {
            historyPeriod = newHistoryPeriod
            refreshLocationHistory(device.address)
        }
    }

    fun onFavoriteClick(device: DeviceData) {
        viewModelScope.launch {
            devicesRepository.changeFavorite(device)
            loadDevice(device.address)
        }
    }

    fun back() {
        router.navigate(BackCommand)
    }

    enum class HistoryPeriod(val periodMills: Long, val displayName: String) {
        DAY(HISTORY_PERIOD_DAY, displayName = "Day"),
        WEEK(HISTORY_PERIOD_WEEK, displayName = "Week"),
        MONTH(HISTORY_PERIOD_MONTH, displayName = "Month"),
        ALL(HISTORY_PERIOD_LONG, displayName = "All time"),
    }

    companion object {
        private const val HISTORY_PERIOD_DAY = 24 * 60 * 60 * 1000L // 24 hours
        private const val HISTORY_PERIOD_WEEK = 7 * 24 * 60 * 60 * 1000L // 1 week
        private const val HISTORY_PERIOD_MONTH = 31 * 24 * 60 * 60 * 1000L // 1 month
        private const val HISTORY_PERIOD_LONG = Long.MAX_VALUE
    }
}