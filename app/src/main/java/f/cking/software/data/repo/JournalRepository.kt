package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.JournalEntry
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JournalRepository(database: AppDatabase) {

    private val journalDao = database.journalDao()
    private val journal = journalDao.observe()
        .map {
            withContext(Dispatchers.Default) {
                it.map { it.toDomain() }
            }
        }

    fun observe(): Flow<List<JournalEntry>> {
        return journal
    }

    suspend fun newEntry(journalEntry: JournalEntry) {
        withContext(Dispatchers.IO) {
            journalDao.insert(journalEntry.toData())
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
}