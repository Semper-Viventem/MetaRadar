package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class JournalRepository(
    private val database: AppDatabase,
) {

    val journalDao = database.journalDao()
    private val journal = MutableStateFlow(emptyList<JournalEntry>())

    suspend fun observe(): Flow<List<JournalEntry>> {
        return journal.apply {
            if (journal.value.isEmpty()) {
                notifyListeners()
            }
        }
    }

    suspend fun newEntry(journalEntry: JournalEntry) {
        withContext(Dispatchers.IO) {
            journalDao.insert(journalEntry.toData())
            notifyListeners()
        }
    }

    suspend fun getAllEntries(): List<JournalEntry> {
        return withContext(Dispatchers.IO) {
            journalDao.getAll().map { it.toDomain() }
        }
    }

    suspend fun getEntryById(id: Int): JournalEntry? {
        return withContext(Dispatchers.IO) {
            journalDao.getById(id)?.toDomain()
        }
    }

    private suspend fun notifyListeners() {
        val data = getAllEntries()
        journal.emit(data)
    }
}