package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.LocationEntity
import f.cking.software.data.database.entity.ProfileDetectEntity
import f.cking.software.data.database.entity.RadarProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RadarProfileDao {

    @Query("SELECT * FROM radar_profile")
    fun getAll(): List<RadarProfileEntity>

    @Query("SELECT * FROM radar_profile")
    fun observe(): Flow<List<RadarProfileEntity>>

    @Query("SELECT * FROM radar_profile WHERE id = :id")
    fun getById(id: Int): RadarProfileEntity?

    @Query("SELECT * FROM radar_profile WHERE id IN (:ids)")
    fun getAllById(ids: List<Int>): List<RadarProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(radarProfile: RadarProfileEntity)

    @Query("DELETE FROM radar_profile WHERE id = :id")
    fun delete(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveProfileDetects(profileDetectEntity: List<ProfileDetectEntity>)

    @Query("SELECT * FROM profile_detect WHERE profile_id = :profileId")
    fun getProfileDetects(profileId: Int): List<ProfileDetectEntity>

    @Query("""
        SELECT l.* 
        FROM location l
        INNER JOIN profile_detect pd ON l.time = pd.trigger_time
        WHERE pd.profile_id = :profileId
    """)
    fun getProfileDetectLocations(profileId: Int): List<LocationEntity>

    @Query("""
        SELECT * FROM profile_detect 
        WHERE profile_id = :profileId 
        ORDER BY trigger_time DESC 
        LIMIT 1
    """)
    fun getLastProfileDetect(profileId: Int): ProfileDetectEntity?
}