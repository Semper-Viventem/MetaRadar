package f.cking.software.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import f.cking.software.data.AppDatabase
import f.cking.software.domain.helpers.BleScannerHelper
import f.cking.software.domain.helpers.PermissionHelper
import f.cking.software.domain.repo.DevicesRepository
import f.cking.software.domain.repo.SettingsRepository
import org.koin.dsl.module

class SingletonsModule(
    private val sharedPreferencesName: String,
    private val appDatabaseName: String,
) {
    val module = module {
        single { BleScannerHelper(get(), get(), get()) }
        single { SettingsRepository(get<Context>().getSharedPreferences(sharedPreferencesName, MODE_PRIVATE)) }
        single { Room.databaseBuilder(get(), AppDatabase::class.java, appDatabaseName).build() }
        single { DevicesRepository(get()) }
        single { PermissionHelper(get()) }
    }
}