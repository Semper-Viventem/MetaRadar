package f.cking.software.ui.devicedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData
import kotlinx.coroutines.launch
import java.util.*

class DeviceDetailsViewModel(
    private val router: NavRouter,
    private val devicesRepository: DevicesRepository
) : ViewModel() {

    var deviceState: Optional<DeviceData> by mutableStateOf(Optional.empty())

    fun loadDevice(address: String) {
        viewModelScope.launch {
            val device = devicesRepository.getDeviceByAddress(address)
            if (device == null) {
                back()
            } else {
                deviceState = Optional.of(device)
            }
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
}