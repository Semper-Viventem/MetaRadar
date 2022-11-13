package f.cking.software.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.helpers.BleScannerHelper
import f.cking.software.domain.helpers.PermissionHelper
import org.koin.dsl.module

class SingletonsModule(
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
    }
}