package f.cking.software.ui.selectfiltertype

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter

class SelectFilterTypeViewModel(
    private val router: NavRouter,
) : ViewModel() {

    val types by mutableStateOf(FilterType.values())

    fun back() {
        router.navigate(BackCommand)
    }

}