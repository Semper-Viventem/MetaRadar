package f.cking.software.ui.devicedetails

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
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
    private val address: String,
    private val router: NavRouter,
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val permissionHelper: PermissionHelper,
) : ViewModel() {

    var deviceState: DeviceData? by mutableStateOf(null)
    var points: List<LocationModel> by mutableStateOf(emptyList())
    var historyPeriod: HistoryPeriod by mutableStateOf(DEFAULT_HISTORY_PERIOD)
    var currentLocation: LocationModel? by mutableStateOf(null)

    init {
        viewModelScope.launch {
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
            refreshLocationHistory(address, autotunePeriod = true)
        }
    }

    private fun observeLocation() {
        permissionHelper.checkBlePermissions {
            viewModelScope.launch {
                locationProvider.fetchOnce()
                locationProvider.observeLocation()
                    .collect { location ->
                        currentLocation = location?.location?.toDomain(System.currentTimeMillis())
                    }
            }
        }
    }

    private suspend fun refreshLocationHistory(address: String, autotunePeriod: Boolean) {
        val fromTime = System.currentTimeMillis() - historyPeriod.periodMills
        val fetched = locationRepository.getAllLocationsByAddress(address, fromTime = fromTime)
        val nextStep = historyPeriod.next()
        val prev = historyPeriod.previous()

        val shouldStepBack = autotunePeriod
                && fetched.size > MAX_POINTS_FOR_AUTO_UPGRADE_PERIOD
                && prev != null

        val shouldStepNext = autotunePeriod && fetched.isEmpty() && nextStep != null

        if (shouldStepBack) {
            selectHistoryPeriodSelected(prev!!, address, autotunePeriod = false)
        } else if (shouldStepNext) {
            selectHistoryPeriodSelected(nextStep!!, address, autotunePeriod)
        }

        points = fetched
    }

    fun selectHistoryPeriodSelected(
        newHistoryPeriod: HistoryPeriod,
        address: String,
        autotunePeriod: Boolean
    ) {
        viewModelScope.launch {
            historyPeriod = newHistoryPeriod
            refreshLocationHistory(address, autotunePeriod = autotunePeriod)
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

    enum class HistoryPeriod(
        val periodMills: Long,
        @StringRes val displayNameRes: Int,
    ) {

        DAY(HISTORY_PERIOD_DAY, displayNameRes = R.string.device_details_day),
        WEEK(HISTORY_PERIOD_WEEK, displayNameRes = R.string.device_details_week),
        MONTH(HISTORY_PERIOD_MONTH, displayNameRes = R.string.device_details_month),
        ALL(HISTORY_PERIOD_LONG, displayNameRes = R.string.device_details_all_time);

        fun next(): HistoryPeriod? {
            return HistoryPeriod.values().getOrNull(ordinal + 1)
        }

        fun previous(): HistoryPeriod? {
            return HistoryPeriod.values().getOrNull(ordinal - 1)
        }
    }

    companion object {
        private const val HISTORY_PERIOD_DAY = 24 * 60 * 60 * 1000L // 24 hours
        private const val HISTORY_PERIOD_WEEK = 7 * 24 * 60 * 60 * 1000L // 1 week
        private const val HISTORY_PERIOD_MONTH = 31 * 24 * 60 * 60 * 1000L // 1 month
        private const val HISTORY_PERIOD_LONG = Long.MAX_VALUE
        private const val MAX_POINTS_FOR_AUTO_UPGRADE_PERIOD = 20_000
        private val DEFAULT_HISTORY_PERIOD = HistoryPeriod.DAY
    }
}