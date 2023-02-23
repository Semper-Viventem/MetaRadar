package f.cking.software.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.*
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val clearGarbageInteractor: ClearGarbageInteractor,
    private val locationRepository: LocationRepository,
    private val locationProvider: LocationProvider,
    private val context: Application,
    private val backupDatabaseInteractor: BackupDatabaseInteractor,
    private val saveReportInteractor: SaveReportInteractor,
    private val createBackupFileInteractor: CreateBackupFileInteractor,
    private val selectBackupFileInteractor: SelectBackupFileInteractor,
    private val restoreDatabaseInteractor: RestoreDatabaseInteractor,
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    var garbageRemovingInProgress: Boolean by mutableStateOf(false)
    var locationRemovingInProgress: Boolean by mutableStateOf(false)
    var backupDbInProgress: Boolean by mutableStateOf(false)
    var useGpsLocationOnly: Boolean by mutableStateOf(settingsRepository.getUseGpsLocationOnly())
    var locationData: LocationProvider.LocationHandle? by mutableStateOf(null)

    init {
        observeLocationData()
    }

    fun onRemoveGarbageClick() {
        viewModelScope.launch {
            garbageRemovingInProgress = true
            val garbageCount = clearGarbageInteractor.execute()
            toast("Cleared $garbageCount garbage devices")
            garbageRemovingInProgress = false
        }
    }

    fun onClearLocationsClick() {
        viewModelScope.launch {
            locationRemovingInProgress = true
            locationRepository.removeAllLocations()
            toast("Location history was removed")
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
                locationProvider.startLocationFetching()
            }
        }
    }

    fun onBackupDBClick() {
        viewModelScope.launch {
            createBackupFileInteractor.execute()
                .catch {
                    toast("Backup has failed. See report in the Journal")
                    reportError(it)
                }
                .collect { uri ->
                    if (uri != null) {
                        backupFileTo(uri)
                    } else {
                        toast("Directory wasn't selected")
                    }
                }
        }
    }

    fun onRestoreDBClick() {
        viewModelScope.launch {
            selectBackupFileInteractor.execute()
                .catch { reportError(it) }
                .collect { uri ->
                    if (uri != null) {
                        restoreFrom(uri)
                    } else {
                        toast("File wasn't selected")
                    }
                }
        }
    }

    private fun observeLocationData() {
        viewModelScope.launch {
            locationProvider.observeLocation()
                .collect { locationHandle ->
                    locationData = locationHandle
                }
        }
    }

    private fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            backupDbInProgress = true
            try {
                restoreDatabaseInteractor.execute(uri)
            } catch (e: Throwable) {
                toast("Cannot restore database. See report in the Journal")
                reportError(e)
            }
            backupDbInProgress = false
            toast("Database was restored from backup")
        }
    }

    private fun backupFileTo(uri: Uri) {
        viewModelScope.launch {
            backupDbInProgress = true
            try {
                backupDatabaseInteractor.execute(uri)
            } catch (e: Throwable) {
                toast("Backup has failed. See report in the Journal")
                reportError(e)
            }
            backupDbInProgress = false
            toast("Backup has succeeded")
        }
    }

    private fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun reportError(error: Throwable) {
        Log.e(TAG, "Settings error", error)
        viewModelScope.launch {
            val report = JournalEntry.Report.Error(
                title = "[Settings]: ${error.message ?: error::class.java}",
                stackTrace = error.stackTraceToString(),
            )
            saveReportInteractor.execute(report)
        }
    }
}