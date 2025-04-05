package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.ProfileDetect
import f.cking.software.domain.model.RadarProfile
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class RadarProfilesRepository(
    database: AppDatabase,
) {
    val dao = database.radarProfileDao()
    private val allProfiles = MutableStateFlow(emptyList<RadarProfile>())

    suspend fun observeAllProfiles(): StateFlow<List<RadarProfile>> =
        withContext(Dispatchers.IO) {
            if (allProfiles.value.isEmpty()) {
                notifyListeners()
            }
            allProfiles
        }

    suspend fun getAllProfiles(): List<RadarProfile> =
        withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
        }

    suspend fun getById(id: Int): RadarProfile? =
        withContext(Dispatchers.IO) {
            dao.getById(id)?.toDomain()
        }

    suspend fun saveProfile(profile: RadarProfile) {
        withContext(Dispatchers.IO) {
            dao.insert(profile.toData())
            notifyListeners()
        }
    }

    suspend fun deleteProfile(profileId: Int) {
        withContext(Dispatchers.IO) {
            dao.delete(profileId)
            notifyListeners()
        }
    }

    suspend fun saveProfileDetects(profileDetects: List<ProfileDetect>) {
        withContext(Dispatchers.IO) {
            dao.saveProfileDetects(profileDetects.map { it.toData() })
        }
    }

    suspend fun getLatestProfileDetect(profileId: Int): ProfileDetect? =
        withContext(Dispatchers.IO) {
            dao.getLastProfileDetect(profileId)?.toDomain()
        }

    suspend fun getProfileDetectLocations(profileId: Int): List<LocationModel> =
        withContext(Dispatchers.IO) {
            dao.getProfileDetectLocations(profileId).map { it.toDomain() }
        }

    private suspend fun notifyListeners() {
        val data = getAllProfiles()
        allProfiles.emit(data)
    }
}
