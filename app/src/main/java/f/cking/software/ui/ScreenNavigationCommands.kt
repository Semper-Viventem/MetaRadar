package f.cking.software.ui

import f.cking.software.common.navigation.AddToStackCommand
import f.cking.software.ui.main.MainScreen
import f.cking.software.ui.profiledetails.ProfileDetailsScreen

object ScreenNavigationCommands {

    object OpenMainScreen : AddToStackCommand(screenFunction = { MainScreen.Screen() })

    data class OpenProfileScreen(val profileId: Int?) : AddToStackCommand(screenFunction = {
        ProfileDetailsScreen.Screen(profileId = profileId)
    })
}