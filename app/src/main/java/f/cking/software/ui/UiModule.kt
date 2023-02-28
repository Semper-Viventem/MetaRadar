package f.cking.software.ui

import f.cking.software.common.navigation.NavRouter
import f.cking.software.ui.devicedetails.DeviceDetailsViewModel
import f.cking.software.ui.devicelist.DeviceListViewModel
import f.cking.software.ui.journal.JournalViewModel
import f.cking.software.ui.main.MainViewModel
import f.cking.software.ui.profiledetails.ProfileDetailsViewModel
import f.cking.software.ui.profileslist.ProfilesListViewModel
import f.cking.software.ui.selectdevice.SelectDeviceViewModel
import f.cking.software.ui.selectmanufacturer.SelectManufacturerViewModel
import f.cking.software.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object UiModule {
    val module = module {
        single { NavRouter() }
        viewModel { MainViewModel(get(), get(), get(), get()) }
        viewModel { DeviceListViewModel(get(), get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { ProfilesListViewModel(get(), get()) }
        viewModel { ProfileDetailsViewModel(profileId = it[0], template = it[1], get(), get(), get()) }
        viewModel { SelectManufacturerViewModel(get()) }
        viewModel { SelectDeviceViewModel(get(), get()) }
        viewModel { DeviceDetailsViewModel(address = it[0], get(), get(), get(), get(), get()) }
        viewModel { JournalViewModel(get(), get(), get(), get(), get()) }
    }
}