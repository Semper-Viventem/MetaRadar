package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.TagEntity

@Dao
interface TagDao {

    @Query("SELECT * FROM tag")
    fun getAll(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tag: TagEntity)

    @Delete
    fun delete(tag: TagEntity)
}