package f.cking.software.ui.devicelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.TheApp
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val devicesRepository: DevicesRepository,
) : ViewModel(), DevicesRepository.OnDevicesUpdateListener {

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
        devicesRepository.addListener(this)
    }

    override fun onDevicesUpdate(devices: List<DeviceData>) {
        viewModelScope.launch(Dispatchers.Main) {
            devicesViewState = devices.sortedWith(generalComparator).reversed()
        }
    }

    fun onDeviceClick(device: DeviceData) {
        viewModelScope.launch(Dispatchers.IO) {
            devicesRepository.changeFavorite(device)
        }
    }

    companion object {
        val factory = viewModelFactory {
            initializer {
                DeviceListViewModel(
                    devicesRepository = TheApp.instance.devicesRepository
                )
            }
        }
    }
}