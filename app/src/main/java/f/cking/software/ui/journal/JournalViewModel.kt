package f.cking.software.ui.journal

import android.app.Application
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.common.navigation.Router
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.JournalRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.JournalEntry
import f.cking.software.ui.ScreenNavigationCommands
import kotlinx.coroutines.launch

class JournalViewModel(
    private val journalRepository: JournalRepository,
    private val profileRepository: RadarProfilesRepository,
    private val devicesRepository: DevicesRepository,
    private val router: Router,
    private val context: Application,
) : ViewModel() {

    var journal: List<JournalEntryUiModel> by mutableStateOf(emptyList())

    init {
        observeJournal()
    }

    fun onEntryClick(journalEntry: JournalEntry) {
        // do nothing
    }

    fun onJournalListItemClick(payload: String?) {
        if (payload != null) {
            router.navigate(ScreenNavigationCommands.OpenDeviceDetailsScreen(payload))
        } else {
            Toast.makeText(context, context.getString(R.string.journal_device_was_removed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeJournal() {
        viewModelScope.launch {
            journalRepository.observe()
                .collect { update ->
                    journal = update.sortedBy { it.timestamp }.reversed().map { map(it) }
                }
        }
    }

    private suspend fun map(from: JournalEntry): JournalEntryUiModel {
        return when (from.report) {
            is JournalEntry.Report.Error -> mapReportError(from, from.report)
            is JournalEntry.Report.ProfileReport -> mapReportProfile(from, from.report)
        }
    }

    private fun mapReportError(
        journalEntry: JournalEntry,
        report: JournalEntry.Report.Error,
    ): JournalEntryUiModel {
        return JournalEntryUiModel(
            dateTime = journalEntry.timestamp.formattedDate(),
            color = R.color.error_background,
            title = report.title,
            subtitle = report.stackTrace,
            journalEntry = journalEntry,
            items = null,
        )
    }

    private suspend fun mapReportProfile(
        journalEntry: JournalEntry,
        report: JournalEntry.Report.ProfileReport,
    ): JournalEntryUiModel {
        return JournalEntryUiModel(
            dateTime = journalEntry.timestamp.formattedDate(),
            color = R.color.profile_report_background,
            title = context.getString(R.string.journal_profile_detected, getProfileName(report.profileId)),
            subtitle = null,
            journalEntry = journalEntry,
            items = mapListItems(report.deviceAddresses),
        )
    }

    private fun Long.formattedDate() = dateTimeStringFormat("dd MMM yyyy, HH:mm")

    private suspend fun getProfileName(id: Int): String {
        return profileRepository.getById(id)?.name ?: context.getString(R.string.unknown_capital_case)
    }

    private suspend fun mapListItems(addresses: List<String>): List<JournalEntryUiModel.ListItemUiModel> {
        val matchedDevices = devicesRepository.getAllByAddresses(addresses)
        return addresses.map { address ->
            val device = matchedDevices.firstOrNull { it.address == address }
            JournalEntryUiModel.ListItemUiModel(
                displayName = device?.buildDisplayName() ?: context.getString(R.string.journal_profile_removed, address),
                payload = device?.address,
            )
        }
    }

    data class JournalEntryUiModel(
        val dateTime: String,
        @ColorRes val color: Int,
        val title: String,
        val subtitle: String?,
        val items: List<ListItemUiModel>?,
        val journalEntry: JournalEntry,
    ) {
        data class ListItemUiModel(
            val displayName: String,
            val payload: String?,
        )
    }
}