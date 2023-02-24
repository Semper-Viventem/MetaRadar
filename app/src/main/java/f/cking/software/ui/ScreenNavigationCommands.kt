package f.cking.software.ui

import f.cking.software.common.navigation.AddToStackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.devicedetails.DeviceDetailsScreen
import f.cking.software.ui.filter.FilterUiState
import f.cking.software.ui.filter.SelectFilterScreen
import f.cking.software.ui.main.MainScreen
import f.cking.software.ui.profiledetails.ProfileDetailsScreen
import f.cking.software.ui.selectdevice.SelectDeviceScreen
import f.cking.software.ui.selectmanufacturer.SelectManufacturerScreen

object ScreenNavigationCommands {

    object OpenMainScreen : AddToStackCommand(screenFunction = { MainScreen.Screen() })

    class OpenProfileScreen(private val profileId: Int?) : AddToStackCommand(screenFunction = { key ->
        ProfileDetailsScreen.Screen(profileId = profileId, key)
    })

    class OpenCreateFilterScreen(
        initialFilterState: FilterUiState,
        router: NavRouter,
        onConfirm: (filterState: RadarProfile.Filter) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectFilterScreen.Screen(initialFilterState, router, onConfirm)
    })

    class OpenSelectManufacturerScreen(
        onSelected: (manufacturerInfo: ManufacturerInfo) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectManufacturerScreen.Screen(onSelected = onSelected)
    })

    class OpenSelectDeviceScreen(
        onSelected: (device: DeviceData) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectDeviceScreen.Screen(onSelected = onSelected)
    })

    class OpenDeviceDetailsScreen(val address: String) : AddToStackCommand(screenFunction = { key ->
        DeviceDetailsScreen.Screen(address = address, key)
    }, key = address)
}