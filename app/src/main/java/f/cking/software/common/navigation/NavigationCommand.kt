package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import java.util.*

interface NavigationCommand

object BackCommand : NavigationCommand

abstract class AddToStackCommand(
    val screenFunction: @Composable (key: String) -> Unit,
    val key: String = UUID.randomUUID().toString()
) : NavigationCommand