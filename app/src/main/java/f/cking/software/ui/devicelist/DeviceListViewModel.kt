package f.cking.software.ui.devicelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.ScreenNavigationCommands
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val devicesRepository: DevicesRepository,
    private val router: NavRouter,
    private val filterCheckerImpl: FilterCheckerImpl,
) : ViewModel() {

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())
    var appliedFilter: List<FilterHolder> by mutableStateOf(emptyList())
    var searchQuery: String? by mutableStateOf(null)
    var isSearchMode: Boolean by mutableStateOf(false)
    var quickFilters: List<FilterHolder> by mutableStateOf(
        listOf(
            DefaultFilters.NOT_APPLE,
            DefaultFilters.IS_FAVORITE,
        )
    )

    private val generalComparator = Comparator<DeviceData> { second, first ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(
                second.lastDetectTimeMs
            )
            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1)
                ?: -1
            first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                first.manufacturerInfo?.name?.compareTo(
                    second.manufacturerInfo?.name ?: return@Comparator 1
                ) ?: -1
            else -> first.address.compareTo(second.address)
        }
    }

    init {
        observeDevices()
    }

    fun onFilterClick(filter: FilterHolder) {
        val newFilters = appliedFilter.toMutableList()
        if (newFilters.contains(filter)) {
            newFilters.remove(filter)
        } else {
            newFilters.add(filter)
        }
        appliedFilter = newFilters
        fetchDevices()
    }

    fun onOpenSearchClick() {
        isSearchMode = !isSearchMode
        if (!isSearchMode) {
            searchQuery = null
        }
        fetchDevices()
    }

    fun onSearchInput(str: String) {
        searchQuery = str
        fetchDevices()
    }

    fun onDeviceClick(device: DeviceData) {
        router.navigate(ScreenNavigationCommands.OpenDeviceDetailsScreen(device.address))
    }

    private fun observeDevices() {
        viewModelScope.launch {
            devicesRepository.observeDevices()
                .collect { devices -> applyDevices(devices) }
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch {
            val devices = devicesRepository.getDevices()
            applyDevices(devices)
        }
    }

    private suspend fun applyDevices(devices: List<DeviceData>) {
        devicesViewState = devices
            .filter { checkFilter(it) && filterQuery(it) }
            .sortedWith(generalComparator)
    }

    private fun filterQuery(device: DeviceData): Boolean {
        return searchQuery?.takeIf { it.isNotBlank() }?.let { searchStr ->
            (device.name?.contains(searchStr, true) ?: false)
                    || (device.customName?.contains(searchStr, true) ?: false)
                    || (device.manufacturerInfo?.name?.contains(searchStr, true) ?: false)
                    || device.address.contains(searchStr, true)
        } ?: true
    }

    private suspend fun checkFilter(device: DeviceData): Boolean {
        val filter = appliedFilter
            .takeIf { it.isNotEmpty() }
            ?.let { RadarProfile.Filter.All(it.map { it.filter }) }

        return if (filter != null) {
            filterCheckerImpl.check(device, filter)
        } else {
            true
        }
    }

    data class FilterHolder(
        val displayName: String,
        val filter: RadarProfile.Filter,
    )

    object DefaultFilters {

        val NOT_APPLE = FilterHolder(
            displayName = "Not apple",
            filter = RadarProfile.Filter.Not(
                filter = RadarProfile.Filter.Manufacturer(ManufacturerInfo.APPLE_ID)
            )
        )

        val IS_FAVORITE = FilterHolder(
            displayName = "Favorite",
            filter = RadarProfile.Filter.IsFavorite(favorite = true)
        )
    }
}