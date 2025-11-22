package f.cking.software.ui.selectdevice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.launch

class SelectDeviceViewModel(
    private val router: Router,
    private val devicesRepository: DevicesRepository,
) : ViewModel() {

    var devices: List<DeviceData> by mutableStateOf(emptyList())
    var loading by mutableStateOf(false)
    var searchStr: String by mutableStateOf("")

    private val generalComparator = Comparator<DeviceData> { second, first ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)
            first.resolvedName != second.resolvedName -> first.resolvedName?.compareTo(second.resolvedName ?: return@Comparator 1) ?: -1
            first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                first.manufacturerInfo?.name?.compareTo(second.manufacturerInfo?.name ?: return@Comparator 1) ?: -1

            else -> first.address.compareTo(second.address)
        }
    }

    init {
        refreshDevices()
    }

    private fun refreshDevices() {
        viewModelScope.launch {
            loading = true
            devices = devicesRepository.getDevices(withAirdropInfo = false).asSequence()
                .filter { device ->
                    searchStr.takeIf { it.isNotBlank() }?.let { searchStr ->
                        (device.resolvedName?.contains(searchStr, true) ?: false)
                                || (device.customName?.contains(searchStr, true) ?: false)
                                || (device.manufacturerInfo?.name?.contains(searchStr, true) ?: false)
                                || device.address.contains(searchStr, true)
                    } ?: true
                }
                .sortedWith(generalComparator)
                .toList()
            loading = false
        }
    }

    fun searchRequest(str: String) {
        searchStr = str
        refreshDevices()
    }

    fun back() {
        router.navigate(BackCommand)
    }
}