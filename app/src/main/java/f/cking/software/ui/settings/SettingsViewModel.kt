package f.cking.software.ui.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.BackupDatabaseInteractor
import f.cking.software.domain.interactor.ClearGarbageInteractor
import f.cking.software.domain.interactor.CreateBackupFileInteractor
import f.cking.software.domain.interactor.RestoreDatabaseInteractor
import f.cking.software.domain.interactor.SaveReportInteractor
import f.cking.software.domain.interactor.SelectBackupFileInteractor
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private val intentHelper: IntentHelper,
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    var garbageRemovingInProgress: Boolean by mutableStateOf(false)
    var locationRemovingInProgress: Boolean by mutableStateOf(false)
    var backupDbInProgress: Boolean by mutableStateOf(false)
    var useGpsLocationOnly: Boolean by mutableStateOf(settingsRepository.getUseGpsLocationOnly())
    var locationData: LocationProvider.LocationHandle? by mutableStateOf(null)
    var runOnStartup: Boolean by mutableStateOf(settingsRepository.getRunOnStartup())

    init {
        observeLocationData()
    }

    fun onRemoveGarbageClick() {
        viewModelScope.launch {
            garbageRemovingInProgress = true
            try {
                val garbageCount = clearGarbageInteractor.execute()
                toast(context.getString(R.string.garbage_has_cleared, garbageCount.toString()))
            } catch (e: Exception) {
                reportError(e)
            }
            garbageRemovingInProgress = false
        }
    }

    fun onClearLocationsClick() {
        viewModelScope.launch {
            locationRemovingInProgress = true
            locationRepository.removeAllLocations()
            toast(context.getString(R.string.settings_location_history_was_removed))
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
            } else {
                locationProvider.fetchOnce()
            }
        }
    }

    fun onBackupDBClick() {
        viewModelScope.launch {
            createBackupFileInteractor.execute()
                .catch {
                    toast(context.getString(R.string.backup_has_failed))
                    reportError(it)
                }
                .collect { uri ->
                    if (uri != null) {
                        backupFileTo(uri)
                    } else {
                        toast(context.getString(R.string.directory_was_not_selected))
                    }
                }
        }
    }

    fun onRestoreDBClick() {
        viewModelScope.launch {
            selectBackupFileInteractor.execute()
                .catch {
                    toast(context.getString(R.string.cannot_restore_database))
                    reportError(it)
                }
                .collect { uri ->
                    if (uri != null) {
                        restoreFrom(uri)
                    } else {
                        toast(context.getString(R.string.file_was_not_selected))
                    }
                }
        }
    }

    fun setRunOnStartup() {
        val newValue = !settingsRepository.getRunOnStartup()
        settingsRepository.setRunOnStartup(newValue)
        runOnStartup = newValue
    }

    fun opReportIssueClick() {
        intentHelper.openUrl(BuildConfig.REPORT_ISSUE_URL)
    }

    fun onGithubClick() {
        intentHelper.openUrl(BuildConfig.GITHUB_URL)
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
                toast(context.getString(R.string.cannot_restore_database))
                reportError(e)
            }
            backupDbInProgress = false
            toast(context.getString(R.string.database_was_restored))
        }
    }

    private fun backupFileTo(uri: Uri) {
        viewModelScope.launch {
            backupDbInProgress = true
            try {
                backupDatabaseInteractor.execute(uri)
            } catch (e: Throwable) {
                toast(context.getString(R.string.backup_has_failed))
                reportError(e)
            }
            backupDbInProgress = false
            toast(context.getString(R.string.backup_has_succeeded))
        }
    }

    private fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun reportError(error: Throwable) {
        Timber.e(error)
        viewModelScope.launch {
            val report = JournalEntry.Report.Error(
                title = "[Settings]: ${error.message ?: error::class.java}",
                stackTrace = error.stackTraceToString(),
            )
            saveReportInteractor.execute(report)
        }
    }
}