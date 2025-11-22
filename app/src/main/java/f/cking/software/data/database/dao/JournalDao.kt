package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(journalEntryEntity: JournalEntryEntity)

    @Query("SELECT * FROM journal")
    fun observe(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal")
    fun getAll(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal WHERE id LIKE :id")
    fun getById(id: Int): JournalEntryEntity?
}