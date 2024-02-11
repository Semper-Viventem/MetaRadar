package f.cking.software.ui.selectmanufacturer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.launch

class SelectManufacturerViewModel(
    private val router: Router,
) : ViewModel() {

    var manufacturers by mutableStateOf(MANUFACTURERS)

    var searchStr by mutableStateOf("")

    init {
        loadManufacturers()
    }

    fun back() {
        router.navigate(BackCommand)
    }

    fun searchRequest(string: String) {
        searchStr = string
        loadManufacturers()
    }

    private fun loadManufacturers() {
        viewModelScope.launch {
            manufacturers = MANUFACTURERS
                .filter { manufacturer ->
                    searchStr.takeIf { it.isNotBlank() }?.let { searchRequest ->
                        manufacturer.name.contains(searchRequest, ignoreCase = true)
                    } ?: true
                }
        }
    }

    private companion object {
        private val MANUFACTURERS by lazy { BluetoothSIG.bluetoothSIG.map { ManufacturerInfo(it.key, it.value, airdrop = null) } }
    }
}