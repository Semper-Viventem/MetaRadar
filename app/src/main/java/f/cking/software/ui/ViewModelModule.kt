package f.cking.software.ui

import f.cking.software.ui.devicelist.DeviceListViewModel
import f.cking.software.ui.main.MainViewModel
import f.cking.software.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object ViewModelModule {
    val module = module {
        viewModel { MainViewModel(get(), get(), get()) }
        viewModel { DeviceListViewModel(get()) }
        viewModel { SettingsViewModel(get()) }
    }
}