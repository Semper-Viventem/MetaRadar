package f.cking.software.common.navigation

import androidx.compose.runtime.Composable

interface NavigationCommand

object BackCommand : NavigationCommand

abstract class AddToStackCommand(val screenFunction: @Composable () -> Unit) : NavigationCommand