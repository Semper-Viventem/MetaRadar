package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.RadarProfile
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadarProfilesRepository(
    database: AppDatabase,
) {

    val dao = database.radarProfileDao()

    suspend fun getAllProfiles(): List<RadarProfile> {
        return withContext(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
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
}