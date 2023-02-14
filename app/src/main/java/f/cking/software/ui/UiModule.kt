package f.cking.software.ui

import f.cking.software.common.navigation.NavRouter
import f.cking.software.ui.devicedetails.DeviceDetailsViewModel
import f.cking.software.ui.devicelist.DeviceListViewModel
import f.cking.software.ui.journal.JournalViewModel
import f.cking.software.ui.main.MainViewModel
import f.cking.software.ui.profiledetails.ProfileDetailsViewModel
import f.cking.software.ui.profileslist.ProfilesListViewModel
import f.cking.software.ui.selectdevice.SelectDeviceViewModel
import f.cking.software.ui.selectfiltertype.SelectFilterTypeViewModel
import f.cking.software.ui.selectmanufacturer.SelectManufacturerViewModel
import f.cking.software.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object UiModule {
    val module = module {
        single { NavRouter() }
        viewModel { MainViewModel(get(), get(), get()) }
        viewModel { DeviceListViewModel(get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
        viewModel { ProfilesListViewModel(get(), get()) }
        viewModel { ProfileDetailsViewModel(get(), get(), get()) }
        viewModel { SelectFilterTypeViewModel(get()) }
        viewModel { SelectManufacturerViewModel(get()) }
        viewModel { SelectDeviceViewModel(get(), get()) }
        viewModel { DeviceDetailsViewModel(get(), get(), get()) }
        viewModel { JournalViewModel(get(), get(), get(), get()) }
    }
}