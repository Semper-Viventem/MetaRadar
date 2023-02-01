package f.cking.software.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(journalEntryEntity: JournalEntryEntity)

    @Query("SELECT * FROM journal")
    fun getAll(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal WHERE id LIKE :id")
    fun getById(id: Int): JournalEntryEntity?
}