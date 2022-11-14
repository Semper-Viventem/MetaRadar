package f.cking.software.ui

import f.cking.software.common.navigation.AddToStackCommand
import f.cking.software.ui.main.MainScreen
import f.cking.software.ui.profiledetails.ProfileDetailsScreen
import f.cking.software.ui.selectfiltertype.FilterType
import f.cking.software.ui.selectfiltertype.SelectFilterTypeScreen

object ScreenNavigationCommands {

    object OpenMainScreen : AddToStackCommand(screenFunction = { MainScreen.Screen() })

    class OpenProfileScreen(private val profileId: Int?) : AddToStackCommand(screenFunction = {
        ProfileDetailsScreen.Screen(profileId = profileId)
    })

    class OpenSelectTypeScreen(
        onTypeSelected: (type: FilterType) -> Unit
    ) : AddToStackCommand(screenFunction = {
        SelectFilterTypeScreen.Screen(onTypeSelected = onTypeSelected)
    })
}