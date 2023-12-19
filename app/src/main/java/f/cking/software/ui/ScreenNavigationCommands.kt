package f.cking.software.ui

import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.devicedetails.DeviceDetailsScreen
import f.cking.software.ui.filter.FilterUiState
import f.cking.software.ui.filter.SelectFilterScreen
import f.cking.software.ui.main.MainScreen
import f.cking.software.ui.profiledetails.ProfileDetailsScreen
import f.cking.software.ui.selectdevice.SelectDeviceScreen
import f.cking.software.ui.selectlocation.SelectLocationScreen
import f.cking.software.ui.selectmanufacturer.SelectManufacturerScreen
import f.cking.software.utils.navigation.AddToStackCommand
import f.cking.software.utils.navigation.BackCommand

object ScreenNavigationCommands {

    object OpenMainScreen : AddToStackCommand(screenFunction = { key, _ -> MainScreen.Screen() })

    class OpenProfileScreen(private val profileId: Int?, private val template: FilterUiState?) : AddToStackCommand(screenFunction = { key, _ ->
        ProfileDetailsScreen.Screen(profileId = profileId, template, key)
    })

    class OpenCreateFilterScreen(
        initialFilterState: FilterUiState,
        onConfirm: (filterState: RadarProfile.Filter) -> Unit
    ) : AddToStackCommand(screenFunction = { key, router ->
        SelectFilterScreen.Screen(initialFilterState, router, onConfirm)
    })

    class OpenSelectManufacturerScreen(
        onSelected: (manufacturerInfo: ManufacturerInfo) -> Unit
    ) : AddToStackCommand(screenFunction = { key, _ ->
        SelectManufacturerScreen.Screen(onSelected = onSelected)
    })

    class OpenSelectDeviceScreen(
        onSelected: (device: DeviceData) -> Unit
    ) : AddToStackCommand(screenFunction = { key, _ ->
        SelectDeviceScreen.Screen(onSelected = onSelected)
    })

    class OpenDeviceDetailsScreen(val address: String) : AddToStackCommand(screenFunction = { key, _ ->
        DeviceDetailsScreen.Screen(address = address, key)
    })

    class OpenSelectLocationScreen(
        initialLocationModel: LocationModel?,
        initialRadius: Float?,
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit
    ) : AddToStackCommand(screenFunction = { key, router ->
        SelectLocationScreen.Screen(
            onSelected = { location, radius ->
                onSelected.invoke(location, radius)
                router.navigate(BackCommand)
            },
            onCloseClick = {
                router.navigate(BackCommand)
            },
            initialLocationModel = initialLocationModel,
            initialRadius = initialRadius,
        )
    })
}