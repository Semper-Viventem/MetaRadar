package f.cking.software.ui.devicedetails

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.interactor.AddTagToDeviceInteractor
import f.cking.software.domain.interactor.ChangeFavoriteInteractor
import f.cking.software.domain.interactor.RemoveTagFromDeviceInteractor
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.toDomain
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class DeviceDetailsViewModel(
    private val address: String,
    private val router: Router,
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val permissionHelper: PermissionHelper,
    private val addTagToDeviceInteractor: AddTagToDeviceInteractor,
    private val removeTagFromDeviceInteractor: RemoveTagFromDeviceInteractor,
    private val changeFavoriteInteractor: ChangeFavoriteInteractor,
) : ViewModel() {

    var deviceState: DeviceData? by mutableStateOf(null)
    var pointsState: List<LocationModel> by mutableStateOf(emptyList())
    var cameraState: MapCameraState by mutableStateOf(DEFAULT_MAP_CAMERA_STATE)
    var historyPeriod by mutableStateOf(DEFAULT_HISTORY_PERIOD)
    var markersInLoadingState by mutableStateOf(false)

    private var currentLocation: LocationModel? = null


    init {
        viewModelScope.launch {
            observeLocation()
            loadDevice(address)
            refreshLocationHistory(address, autotunePeriod = true)
        }
    }

    private suspend fun loadDevice(address: String) {
        val device = devicesRepository.getDeviceByAddress(address)
        if (device == null) {
            back()
        } else {
            deviceState = device
        }
    }

    private fun observeLocation() {
        permissionHelper.checkBlePermissions {
            viewModelScope.launch {
                locationProvider.fetchOnce()
                locationProvider.observeLocation()
                    .take(2)
                    .collect { location ->
                        currentLocation = location?.location?.toDomain(System.currentTimeMillis())
                        updateCameraPosition(pointsState, currentLocation)
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

        pointsState = fetched
        updateCameraPosition(pointsState, currentLocation)
    }

    private fun updateCameraPosition(points: List<LocationModel>, currentLocation: LocationModel?) {
        val previousState: MapCameraState = cameraState
        val withAnimation = previousState != DEFAULT_MAP_CAMERA_STATE
        val newState = if (points.isNotEmpty()) {
            MapCameraState.MultiplePoints(points, withAnimation = withAnimation)
        } else if (currentLocation != null) {
            MapCameraState.SinglePoint(location = currentLocation, zoom = MapConfig.DEFAULT_MAP_ZOOM, withAnimation = withAnimation)
        } else {
            DEFAULT_MAP_CAMERA_STATE.copy(withAnimation = withAnimation)
        }
        if (newState != previousState) {
            cameraState = newState
        }
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
            changeFavoriteInteractor.execute(device)
            loadDevice(device.address)
        }
    }

    fun onNewTagSelected(device: DeviceData, tag: String) {
        viewModelScope.launch {
            addTagToDeviceInteractor.execute(device, tag)
            loadDevice(deviceState!!.address)
        }
    }

    fun onRemoveTagClick(device: DeviceData, tag: String) {
        viewModelScope.launch {
            removeTagFromDeviceInteractor.execute(device, tag)
            loadDevice(deviceState!!.address)
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

    sealed interface MapCameraState {
        data class SinglePoint(
            val location: LocationModel,
            val zoom: Double,
            val withAnimation: Boolean,
        ) : MapCameraState

        data class MultiplePoints(
            val points: List<LocationModel>,
            val withAnimation: Boolean,
        ) : MapCameraState
    }

    companion object {
        private const val HISTORY_PERIOD_DAY = 24 * 60 * 60 * 1000L // 24 hours
        private const val HISTORY_PERIOD_WEEK = 7 * 24 * 60 * 60 * 1000L // 1 week
        private const val HISTORY_PERIOD_MONTH = 31 * 24 * 60 * 60 * 1000L // 1 month
        private const val HISTORY_PERIOD_LONG = Long.MAX_VALUE
        private const val MAX_POINTS_FOR_AUTO_UPGRADE_PERIOD = 20_000
        private val DEFAULT_HISTORY_PERIOD = HistoryPeriod.DAY

        private val DEFAULT_MAP_CAMERA_STATE = MapCameraState.SinglePoint(
            location = LocationModel(0.0, 0.0, 0),
            zoom = MapConfig.MIN_MAP_ZOOM,
            withAnimation = false
        )
    }
}