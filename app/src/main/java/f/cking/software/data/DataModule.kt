package f.cking.software.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.helpers.*
import f.cking.software.data.repo.*
import org.koin.dsl.module

class DataModule(
    private val sharedPreferencesName: String,
    private val appDatabaseName: String,
) {
    val module = module {
        single { BleScannerHelper(get(), get(), get()) }
        single { get<Context>().getSharedPreferences(sharedPreferencesName, MODE_PRIVATE) }
        single { SettingsRepository(get()) }
        single { AppDatabase.build(get(), appDatabaseName) }
        single { DevicesRepository(get()) }
        single { PermissionHelper(get(), get()) }
        single { ActivityProvider() }
        single { IntentHelper(get()) }
        single { RadarProfilesRepository(get()) }
        single { LocationProvider(get(), get(), get()) }
        single { LocationRepository(get()) }
        single { JournalRepository(get()) }
    }
}