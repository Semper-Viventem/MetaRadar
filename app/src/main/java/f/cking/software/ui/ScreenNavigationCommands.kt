package f.cking.software.ui

import f.cking.software.common.navigation.AddToStackCommand
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.ui.main.MainScreen
import f.cking.software.ui.profiledetails.ProfileDetailsScreen
import f.cking.software.ui.selectfiltertype.FilterType
import f.cking.software.ui.selectfiltertype.SelectFilterTypeScreen
import f.cking.software.ui.selectmanufacturer.SelectManufacturerScreen

object ScreenNavigationCommands {

    object OpenMainScreen : AddToStackCommand(screenFunction = { MainScreen.Screen() })

    class OpenProfileScreen(private val profileId: Int?) : AddToStackCommand(screenFunction = {
        ProfileDetailsScreen.Screen(profileId = profileId)
    })

    class OpenSelectFilterTypeScreen(
        onSelected: (type: FilterType) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectFilterTypeScreen.Screen(onSelected = onSelected)
    })

    class OpenSelectManufacturerScreen(
        onSelected: (type: ManufacturerInfo) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectManufacturerScreen.Screen(onSelected = onSelected)
    })
}