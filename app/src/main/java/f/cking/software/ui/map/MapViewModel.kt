package f.cking.software.ui.map

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.data.helpers.ActivityProvider
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.repo.SettingsRepository
import kotlinx.coroutines.launch

class MapViewModel(
    private val settingsRepository: SettingsRepository,
    private val intentHelper: IntentHelper,
    private val activityProvider: ActivityProvider,
) : ViewModel() {

    var silentModeEnabled by mutableStateOf(settingsRepository.getSilentMode())

    init {
        viewModelScope.launch {
            settingsRepository.observeSilentMode()
                .collect { silentModeEnabled = it }
        }
    }

    fun openOSMLicense() {
        intentHelper.openUrl(BuildConfig.MAP_LICENSE_URL)
    }

    fun changeSilentModeState() {
        settingsRepository.setSilentMode(!settingsRepository.getSilentMode())
        Toast.makeText(activityProvider.requireActivity(), R.string.silent_mode_is_off_toast, Toast.LENGTH_LONG).show()
    }
}