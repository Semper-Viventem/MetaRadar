package f.cking.software.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.TheApp
import f.cking.software.domain.repo.SettingsRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    companion object {
        val factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = TheApp.instance.settingsRepository
                )
            }
        }
    }
}