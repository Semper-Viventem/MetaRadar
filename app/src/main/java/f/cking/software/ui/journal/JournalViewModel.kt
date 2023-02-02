package f.cking.software.ui.journal

import androidx.annotation.ColorRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.JournalRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.launch

class JournalViewModel(
    private val journalRepository: JournalRepository,
    private val profileRepository: RadarProfilesRepository,
    private val devicesRepository: DevicesRepository,
    private val router: NavRouter,
) : ViewModel() {

    var journal: List<JournalEntryUiModel> by mutableStateOf(emptyList())

    init {
        observeJournal()
    }

    fun onEntryClick(journalEntry: JournalEntry) {

    }

    private fun observeJournal() {
        viewModelScope.launch {
            journalRepository.observe()
                .collect { update -> journal = update.sortedBy { it.timestamp }.reversed().map { map(it) } }
        }
    }

    private suspend fun map(from: JournalEntry): JournalEntryUiModel {
        return JournalEntryUiModel(
            dateTime = from.timestamp.dateTimeStringFormat("dd MMM yyyy, HH:MM"),
            color = when (from.report) {
                is JournalEntry.Report.Error -> R.color.error_background
                is JournalEntry.Report.ProfileReport -> R.color.profile_report_background
            },
            title = when (from.report) {
                is JournalEntry.Report.Error -> from.report.title
                is JournalEntry.Report.ProfileReport -> "Profile detected: \"${getProfileName(from.report.profileId)}\""
            },
            subtitle = when (from.report) {
                is JournalEntry.Report.Error -> from.report.stackTrace
                is JournalEntry.Report.ProfileReport -> getDeviceNameByAddresses(from.report.deviceAddresses)
            },
            journalEntry = from,
        )
    }

    private suspend fun getProfileName(id: Int): String {
        return profileRepository.getById(id)?.name ?: "UNKNOWN"
    }

    private suspend fun getDeviceNameByAddresses(addresses: List<String>): String {
        return devicesRepository.getAllByAddresses(addresses).joinToString { it.buildDisplayName() }
    }

    data class JournalEntryUiModel(
        val dateTime: String,
        @ColorRes val color: Int,
        val title: String,
        val subtitle: String,
        val journalEntry: JournalEntry,
    )
}