package f.cking.software.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.helpers.ActivityProvider
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.NotificationsHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.helpers.PowerModeHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.JournalRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.data.repo.TagsRepository
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
        single { PermissionHelper(get(), get(), get()) }
        single { ActivityProvider() }
        single { IntentHelper(get()) }
        single { RadarProfilesRepository(get()) }
        single { LocationProvider(get(), get(), get(), get()) }
        single { LocationRepository(get()) }
        single { JournalRepository(get()) }
        single { NotificationsHelper(get(), get()) }
        single { PowerModeHelper(get()) }
        single { TagsRepository(get()) }
    }
}