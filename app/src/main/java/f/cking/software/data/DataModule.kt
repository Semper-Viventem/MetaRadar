package f.cking.software.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.data.repo.SettingsRepository
import org.koin.dsl.module

class DataModule(
    private val sharedPreferencesName: String,
    private val appDatabaseName: String,
) {
    val module = module {
        single { BleScannerHelper(get(), get(), get()) }
        single { SettingsRepository(get<Context>().getSharedPreferences(sharedPreferencesName, MODE_PRIVATE)) }
        single { AppDatabase.build(get(), appDatabaseName) }
        single { DevicesRepository(get()) }
        single { PermissionHelper(get()) }
        single { RadarProfilesRepository(get()) }
        single { LocationProvider(get()) }
        single { LocationRepository(get()) }
    }
}