package f.cking.software

import android.app.Application
import f.cking.software.data.DataModule
import f.cking.software.domain.interactor.InteractorsModule
import f.cking.software.ui.UiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class TheApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDi()
    }

    private fun initDi() {
        startKoin {
            androidContext(this@TheApp)
            modules(
                DataModule(SHARED_PREF_NAME, DATABASE_NAME).module,
                InteractorsModule.module,
                UiModule.module,
            )
        }
    }

    fun restartKoin() {
        stopKoin()
        initDi()
    }

    companion object {
        lateinit var instance: TheApp

        const val SHARED_PREF_NAME = "app-prefs"
        const val DATABASE_NAME = "app-database"
    }
}