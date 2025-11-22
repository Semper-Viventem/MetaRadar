package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.ProfileDetect
import f.cking.software.domain.model.RadarProfile
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RadarProfilesRepository(
    database: AppDatabase,
) {

    val dao = database.radarProfileDao()
    private val allProfiles = dao.observe()
        .map {
            withContext(Dispatchers.Default) {
                it.map { it.toDomain() }
            }
        }

    suspend fun observeAllProfiles(): Flow<List<RadarProfile>> {
        return allProfiles
    }

    suspend fun getAllProfiles(): List<RadarProfile> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
        }
    }

    suspend fun getById(id: Int): RadarProfile? {
        return withContext(Dispatchers.IO) {
            dao.getById(id)?.toDomain()
        }
    }

    suspend fun getAllByIds(ids: List<Int>): List<RadarProfile> {
        return withContext(Dispatchers.IO) {
            dao.getAllById(ids).map { it.toDomain() }
        }
    }

    suspend fun saveProfile(profile: RadarProfile) {
        withContext(Dispatchers.IO) {
            dao.insert(profile.toData())
        }
    }

    suspend fun deleteProfile(profileId: Int) {
        withContext(Dispatchers.IO) {
            dao.delete(profileId)
        }
    }

    suspend fun saveProfileDetects(profileDetects: List<ProfileDetect>) {
        withContext(Dispatchers.IO) {
            dao.saveProfileDetects(profileDetects.map { it.toData() })
        }
    }

    suspend fun getLatestProfileDetect(profileId: Int): ProfileDetect? {
        return withContext(Dispatchers.IO) {
            dao.getLastProfileDetect(profileId)?.toDomain()
        }
    }

    suspend fun getProfileDetectLocations(profileId: Int): List<LocationModel> {
        return withContext(Dispatchers.IO) {
            dao.getProfileDetectLocations(profileId).map { it.toDomain() }
        }
    }
}