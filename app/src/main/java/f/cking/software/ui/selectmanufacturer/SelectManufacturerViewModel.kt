package f.cking.software.ui.selectmanufacturer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router

class SelectManufacturerViewModel(
    private val router: Router,
) : ViewModel() {

    val manufacturers by mutableStateOf(
        BluetoothSIG.bluetoothSIG.map { ManufacturerInfo(it.key, it.value, airdrop = null) }
    )

    fun back() {
        router.navigate(BackCommand)
    }
}