package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import com.vanpra.composematerialdialogs.MaterialDialogState

interface NavigationCommand

object BackCommand : NavigationCommand

abstract class AddToStackCommand(val screenFunction: @Composable () -> Unit) : NavigationCommand

abstract class DialogCommand(
    val dialogProvider: @Composable (onClose: () -> Unit) -> MaterialDialogState
) : NavigationCommand