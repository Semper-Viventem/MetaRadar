package f.cking.software.ui.settings

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.domain.interactor.ClearGarbageInteractor
import f.cking.software.domain.repo.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val clearGarbageInteractor: ClearGarbageInteractor,
    private val context: Application,
) : ViewModel() {

    var garbageRemovingInProgress: Boolean by mutableStateOf(false)

    fun onRemoveGarbageClick() {
        viewModelScope.launch {
            garbageRemovingInProgress = true
            val garbageCount = clearGarbageInteractor.execute()
            Toast.makeText(context, "Cleared $garbageCount garbage devices", Toast.LENGTH_SHORT).show()
            garbageRemovingInProgress = false
        }
    }
}