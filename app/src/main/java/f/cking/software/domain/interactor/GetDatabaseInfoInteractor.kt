package f.cking.software.domain.interactor

import f.cking.software.TheApp
import f.cking.software.data.database.AppDatabase
import f.cking.software.domain.model.DatabaseInformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetDatabaseInfoInteractor(
    private val database: AppDatabase,
    private val application: TheApp,
) {
    fun execute(): Flow<DatabaseInformation> {
        return combine(
            database.deviceDao().observeAll().map { it.size },
            database.locationDao().observeAllLocations().map { it.size }
        ) { deviceCount, locationCount ->
            DatabaseInformation(
                sizeBytes = database.getDatabaseSize(application),
                totalDevices = deviceCount,
                totalGeotags = locationCount
            )
        }
    }
}