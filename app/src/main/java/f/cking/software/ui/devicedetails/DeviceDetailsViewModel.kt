package f.cking.software.ui.devicedetails

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.helpers.PowerModeHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.interactor.AddTagToDeviceInteractor
import f.cking.software.domain.interactor.ChangeFavoriteInteractor
import f.cking.software.domain.interactor.FetchDeviceServiceInfo
import f.cking.software.domain.interactor.GetBleRecordFramesFromRawInteractor
import f.cking.software.domain.interactor.GetCharacteristicNameFromUUID
import f.cking.software.domain.interactor.GetServiceNameFromBluetoothService
import f.cking.software.domain.interactor.RemoveTagFromDeviceInteractor
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.toDomain
import f.cking.software.fromBase64
import f.cking.software.service.BgScanService
import f.cking.software.toBase64
import f.cking.software.toHexString
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

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
    private val bleScannerHelper: BleScannerHelper,
    private val getBleRecordFramesFromRawInteractor: GetBleRecordFramesFromRawInteractor,
    private val fetchDeviceServiceInfo: FetchDeviceServiceInfo,
) : ViewModel() {

    var deviceState: DeviceData? by mutableStateOf(null)
    var pointsState: List<LocationModel> by mutableStateOf(emptyList())
    var cameraState: MapCameraState by mutableStateOf(DEFAULT_MAP_CAMERA_STATE)
    var historyPeriod by mutableStateOf(DEFAULT_HISTORY_PERIOD)
    var markersInLoadingState by mutableStateOf(false)
    var loadingHeatmap by mutableStateOf(false)
    var onlineStatusData: OnlineStatus? by mutableStateOf(null)
    var pointsStyle: PointsStyle by mutableStateOf(DEFAULT_POINTS_STYLE)
    var rawData: List<Pair<String, String>> by mutableStateOf(listOf())
    var services: Set<ServiceData> by mutableStateOf(emptySet())
    var connectionStatus: ConnectionStatus by mutableStateOf(ConnectionStatus.DISCONNECTED)
    private var connectionJob: Job? = null
    var matadataIsFetching by mutableStateOf(false)

    var mapExpanded: Boolean by mutableStateOf(false)

    var useHeatmap: Boolean by mutableStateOf(true)

    sealed class ConnectionStatus(@StringRes val statusRes: Int) {
        data class CONNECTED(val gatt: BluetoothGatt) : ConnectionStatus(R.string.device_details_status_connected)
        data object CONNECTING : ConnectionStatus(R.string.device_details_status_connecting)
        data object DISCONNECTED : ConnectionStatus(R.string.device_details_status_disconnected)
        data object DISCONNECTING : ConnectionStatus(R.string.device_details_status_disconnecting)
    }

    data class ServiceData(
        val name: String?,
        val uuid: String,
        val characteristics: List<CharacteristicData>,
    )

    data class CharacteristicData(
        val name: String?,
        val uuid: String,
        val value: String?,
        val valueHex: String?,
        val encodedValue: String?,
        val gatt: BluetoothGattCharacteristic,
    )

    private var currentLocation: LocationModel? = null

    init {
        viewModelScope.launch {
            observeLocation()
            loadDevice(address)
            observeOnlineStatus()
            refreshLocationHistory(address, autotunePeriod = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionJob?.cancel()
    }

    fun establishConnection() {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            bleScannerHelper.connectToDevice(address)
                .onStart { connectionStatus = ConnectionStatus.CONNECTING }
                .catch { e ->
                    Timber.e(e)
                    connectionStatus = ConnectionStatus.DISCONNECTED
                }
                .collect { result ->
                    handleBleConnectResult(result)
                }
        }
    }

    private fun handleBleConnectResult(result: BleScannerHelper.DeviceConnectResult) {
        when (result) {
            is BleScannerHelper.DeviceConnectResult.Connected -> {
                connectionStatus = ConnectionStatus.CONNECTED(result.gatt)
                discoverServices(result.gatt)
            }

            is BleScannerHelper.DeviceConnectResult.Connecting -> {
                connectionStatus = ConnectionStatus.CONNECTING
            }

            is BleScannerHelper.DeviceConnectResult.Disconnected -> {
                connectionStatus = ConnectionStatus.DISCONNECTED
                connectionJob?.cancel()
            }

            is BleScannerHelper.DeviceConnectResult.Disconnecting -> {
                connectionStatus = ConnectionStatus.DISCONNECTING
            }

            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError -> {
                Timber.e(RuntimeException("Error while connecting to device, error code ${result.errorCode}"))
                connectionStatus = ConnectionStatus.DISCONNECTED
                connectionJob?.cancel()
            }

            // services update
            is BleScannerHelper.DeviceConnectResult.AvailableServices -> {
                addServices(result.services.map { mapService(it) }.toSet())
                result.services.forEach { it.characteristics.forEach { readDescription(it) } }
            }

            is BleScannerHelper.DeviceConnectResult.CharacteristicRead -> {
                val updatedServices = services.map { service ->
                    val updatedCharacteristics = service.characteristics.map { characteristic ->
                        if (characteristic.uuid == result.characteristic.uuid.toString()) {
                            mapCharacteristic(result.characteristic, result.valueEncoded64.fromBase64())
                        } else {
                            characteristic
                        }
                    }
                    service.copy(characteristics = updatedCharacteristics)
                }
                addServices(updatedServices.toSet())
            }

            is BleScannerHelper.DeviceConnectResult.FailedReadCharacteristic -> {
                // do nothing
            }

            is BleScannerHelper.DeviceConnectResult.DescriptorRead -> {
                val updatedServices = services.map { service ->
                    val updatedCharacteristics = service.characteristics.map { characteristic ->
                        if (characteristic.gatt.descriptors.any { it.uuid == result.descriptor.uuid }) {
                            mapCharacteristic(characteristic.gatt, characteristic.encodedValue?.fromBase64(), result.valueEncoded64.fromBase64())
                        } else {
                            characteristic
                        }
                    }
                    service.copy(characteristics = updatedCharacteristics)
                }
                addServices(updatedServices.toSet())
            }
        }
    }

    private fun mapCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray? = null,
        description: ByteArray? = null
    ): CharacteristicData {
        val valueStr = value?.decodeToString()
        val valueHex = value?.toHexString()?.uppercase()?.let { "0x$it" }
        return CharacteristicData(
            name = description?.decodeToString() ?: getCharacteristicNameIfKnown(characteristic),
            uuid = characteristic.uuid.toString(),
            value = valueStr,
            valueHex = valueHex,
            encodedValue = value?.toBase64(),
            gatt = characteristic
        )
    }

    private fun mapService(service: BluetoothGattService): ServiceData {
        return ServiceData(getServiceNameIfKnown(service), service.uuid.toString(), service.characteristics.map { mapCharacteristic(it) })
    }

    private fun getServiceNameIfKnown(service: BluetoothGattService): String? {
        return GetServiceNameFromBluetoothService.execute(service.uuid.toString())
    }

    private fun getCharacteristicNameIfKnown(characteristic: BluetoothGattCharacteristic): String? {
        return GetCharacteristicNameFromUUID.execute(characteristic.uuid.toString())
    }

    fun readDescription(characteristic: BluetoothGattCharacteristic) {
        viewModelScope.launch {
            val gat = (connectionStatus as? ConnectionStatus.CONNECTED)?.gatt
            val descriptor = characteristic.descriptors.firstOrNull { it.uuid == UUID.fromString(DESCRIPTOR_CHARACTERISTIC_USER_DESCRIPTION) }
            if (gat != null && descriptor != null) {
                try {
                    bleScannerHelper.readDescriptor(gat, characteristic, descriptor.uuid)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun fetchDeviceServiceInfo(device: DeviceData) {
        viewModelScope.launch {
            try {
                matadataIsFetching = true
                fetchDeviceServiceInfo.execute(device)
                loadDevice(address)
            } catch (e: Exception) {
                Timber.tag("FetchDeviceServiceInfo").e(e)
            } finally {
                matadataIsFetching = false
            }
        }
    }

    fun readCharacteristic(gattService: BluetoothGattCharacteristic) {
        viewModelScope.launch {
            (connectionStatus as? ConnectionStatus.CONNECTED)?.gatt?.let { gatt ->
                try {
                    bleScannerHelper.readCharacteristic(gatt, gattService)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    fun discoverServices(gatt: BluetoothGatt) {
        viewModelScope.launch {
            try {
                bleScannerHelper.discoverServices(gatt)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun disconnect(gatt: BluetoothGatt) {
        viewModelScope.launch {
            try {
                bleScannerHelper.disconnect(gatt)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun loadRawData(raw: ByteArray) {
        val frames = getBleRecordFramesFromRawInteractor.execute(raw)
        rawData = frames.map {
            it.type.toHexString().uppercase() to it.data.toHexString().uppercase()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeOnlineStatus() {
        viewModelScope.launch {
            BgScanService.observeIsActive()
                .flatMapLatest { isActive ->
                    if (isActive) {
                        devicesRepository.observeLastBatch()
                    } else {
                        flowOf(emptyList())
                    }
                }
                .map { devices ->
                    val currentDevice = devices.firstOrNull { it.address == address }
                    val rssi = currentDevice?.rssi
                    val distance = currentDevice?.distance()
                    if (rssi != null && distance != null) {
                        deviceState = currentDevice
                        OnlineStatus(rssi, distance)
                    } else if (connectionStatus !is ConnectionStatus.DISCONNECTED) {
                        OnlineStatus(null, null)
                    } else {
                        null
                    }
                }
                .collect { onlineStatus ->
                    onlineStatusData = onlineStatus
                }
        }
    }

    private suspend fun loadDevice(address: String) {
        val device = devicesRepository.getDeviceByAddress(address)
        if (device == null) {
            back()
        } else {
            device.rowDataEncoded?.fromBase64()?.let { loadRawData(it) }
            addServices(device.servicesUuids.map { ServiceData(null, it, emptyList()) }.toSet())
            deviceState = device
        }
    }

    private fun addServices(servicesUuids: Set<ServiceData>) {
        services = (services + servicesUuids).associateBy { it.uuid }.values.toSet()
    }

    private fun observeLocation() {
        permissionHelper.checkOrRequestPermission {
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

        val shouldStepNext = autotunePeriod && fetched.isEmpty() && nextStep != null

        if (shouldStepNext) {
            selectHistoryPeriodSelected(nextStep, address, autotunePeriod)
        }

        if (fetched.size > MAX_POINTS_FOR_MARKERS) {
            pointsStyle = PointsStyle.PATH
        }

        if (fetched.size > MAX_POINTS_FOR_HEATMAP) {
            useHeatmap = false
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

    enum class PointsStyle(@StringRes val displayNameRes: Int) {
        MARKERS(R.string.device_history_pint_style_markers),
        PATH(R.string.device_history_pint_style_path),
        HIDE_MARKERS(R.string.device_history_pint_style_hide_markers),
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

    data class OnlineStatus(
        val signalStrength: Int?,
        val distance: Float?,
    )

    companion object {
        private const val DESCRIPTOR_CHARACTERISTIC_USER_DESCRIPTION = "00002901-0000-1000-8000-00805f9b34fb"
        private const val MAX_POINTS_FOR_MARKERS = 5_000
        private const val MAX_POINTS_FOR_HEATMAP = 30_000
        private const val HISTORY_PERIOD_DAY = 24 * 60 * 60 * 1000L // 24 hours
        private const val HISTORY_PERIOD_WEEK = 7 * 24 * 60 * 60 * 1000L // 1 week
        private const val HISTORY_PERIOD_MONTH = 31 * 24 * 60 * 60 * 1000L // 1 month
        private const val HISTORY_PERIOD_LONG = Long.MAX_VALUE
        private val DEFAULT_HISTORY_PERIOD = HistoryPeriod.DAY
        private val ONLINE_THRESHOLD_MS =
            PowerModeHelper.PowerMode.POWER_SAVING.scanDuration + PowerModeHelper.PowerMode.POWER_SAVING.scanDuration + 3000L
        private val DEFAULT_POINTS_STYLE = PointsStyle.MARKERS

        private val DEFAULT_MAP_CAMERA_STATE = MapCameraState.SinglePoint(
            location = LocationModel(0.0, 0.0, 0),
            zoom = MapConfig.MIN_MAP_ZOOM,
            withAnimation = false
        )
    }
}