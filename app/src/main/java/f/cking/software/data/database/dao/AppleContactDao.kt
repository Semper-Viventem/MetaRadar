package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.AppleContactEntity

@Dao
interface AppleContactDao {

    @Query("SELECT * FROM apple_contacts")
    fun getAll(): List<AppleContactEntity>

    @Query("SELECT * FROM apple_contacts WHERE associated_address LIKE :address")
    fun getByAddress(address: String): List<AppleContactEntity>

    @Query("SELECT * FROM apple_contacts WHERE sha_256 IN (:sha)")
    fun getBySHA(sha: List<Int>): List<AppleContactEntity>

    @Query("SELECT * FROM apple_contacts WHERE associated_address IN (:addresses)")
    fun getByAddresses(addresses: List<String>): List<AppleContactEntity>

    @Query("SELECT * FROM apple_contacts WHERE last_detect_time_ms LIKE :scanTime")
    fun getByScanTime(scanTime: Long): List<AppleContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: AppleContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(contacts: List<AppleContactEntity>)

    @Query("DELETE FROM apple_contacts WHERE associated_address IN (:addresses)")
    fun deleteAllByAddresses(addresses: List<String>)
}