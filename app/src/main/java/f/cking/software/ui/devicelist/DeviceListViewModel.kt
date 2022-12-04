package f.cking.software.ui.devicelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.ui.ScreenNavigationCommands
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val devicesRepository: DevicesRepository,
    private val router: NavRouter,
) : ViewModel() {

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())

    private val generalComparator = Comparator<DeviceData> { first, second ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)
            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1) ?: -1
            first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                first.manufacturerInfo?.name?.compareTo(second.manufacturerInfo?.name ?: return@Comparator 1) ?: -1
            else -> first.address.compareTo(second.address)
        }
    }

    init {
        observeDevices()
    }

    fun onDeviceClick(device: DeviceData) {
        router.navigate(ScreenNavigationCommands.OpenDeviceDetailsScreen(device.address))
    }

    private fun observeDevices() {
        viewModelScope.launch {
            devicesRepository.observeDevices()
                .collect { devices ->
                    devicesViewState = devices.sortedWith(generalComparator).reversed()
                }
        }
    }
}