package f.cking.software.domain.interactor

import f.cking.software.data.repo.JournalRepository
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveReportInteractor(
    private val journalRepository: JournalRepository,
) {

    suspend fun execute(report: JournalEntry.Report) {
        withContext(Dispatchers.Default) {
            val journalEntry = JournalEntry(
                id = null,
                timestamp = System.currentTimeMillis(),
                report = report,
            )

            journalRepository.newEntry(journalEntry)
        }
    }
}