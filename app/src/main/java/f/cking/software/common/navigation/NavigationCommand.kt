package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import java.util.UUID

interface NavigationCommand

object BackCommand : NavigationCommand

abstract class AddToStackCommand(
    val screenFunction: @Composable (key: String, router: Router) -> Unit,
    val key: String = UUID.randomUUID().toString()
) : NavigationCommand