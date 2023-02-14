package f.cking.software.ui.settings

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.ClearGarbageInteractor
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val clearGarbageInteractor: ClearGarbageInteractor,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val context: Application,
) : ViewModel() {

    var garbageRemovingInProgress: Boolean by mutableStateOf(false)
    var locationRemovingInProgress: Boolean by mutableStateOf(false)
    var useGpsLocationOnly: Boolean by mutableStateOf(settingsRepository.getUseGpsLocationOnly())

    fun onRemoveGarbageClick() {
        viewModelScope.launch {
            garbageRemovingInProgress = true
            val garbageCount = clearGarbageInteractor.execute()
            Toast.makeText(context, "Cleared $garbageCount garbage devices", Toast.LENGTH_SHORT).show()
            garbageRemovingInProgress = false
        }
    }

    fun onClearLocationsClick() {
        viewModelScope.launch {
            locationRemovingInProgress = true
            locationRepository.removeAllLocations()
            Toast.makeText(context, "Location history was removed", Toast.LENGTH_SHORT).show()
            locationRemovingInProgress = false
        }
    }

    fun onUseGpsLocationOnlyClick() {
        viewModelScope.launch {
            val currentValue = settingsRepository.getUseGpsLocationOnly()
            settingsRepository.setUseGpsLocationOnly(!currentValue)
            useGpsLocationOnly = !currentValue

            // restart location provider
            if (locationProvider.isActive()) {
                locationProvider.stopLocationListening()
                locationProvider.startLocationLeastening()
            }
        }
    }
}