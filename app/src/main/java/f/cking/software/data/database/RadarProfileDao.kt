package f.cking.software.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RadarProfileDao {

    @Query("SELECT * FROM radar_profile")
    fun getAll(): List<RadarProfileEntity>

    @Query("SELECT * FROM radar_profile WHERE id LIKE :id")
    fun getById(id: Int): RadarProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(radarProfile: RadarProfileEntity)

    @Query("DELETE FROM radar_profile WHERE id LIKE :id")
    fun delete(id: Int)
}