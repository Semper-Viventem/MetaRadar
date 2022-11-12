package f.cking.software.ui.devicelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val devicesRepository: DevicesRepository,
) : ViewModel() {

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())

    private val generalComparator = Comparator<DeviceData> { first, second ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)
            first.detectCount != second.detectCount -> first.detectCount.compareTo(second.detectCount)
            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1) ?: -1
            first.firstDetectTimeMs != second.firstDetectTimeMs -> second.firstDetectTimeMs.compareTo(first.firstDetectTimeMs)
            else -> first.address.compareTo(second.address)
        }
    }

    init {
        observeDevices()
    }

    fun onDeviceClick(device: DeviceData) {
        viewModelScope.launch(Dispatchers.IO) {
            devicesRepository.changeFavorite(device)
        }
    }

    private fun observeDevices() {
        viewModelScope.launch() {
            devicesRepository.observeDevices()
                .collect { devices ->
                    devicesViewState = devices.sortedWith(generalComparator).reversed()
                }
        }
    }
}