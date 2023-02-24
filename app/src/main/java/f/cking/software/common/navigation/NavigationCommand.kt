package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import com.vanpra.composematerialdialogs.MaterialDialogState
import java.util.*

interface NavigationCommand

object BackCommand : NavigationCommand

abstract class AddToStackCommand(val screenFunction: @Composable (key: String) -> Unit, val key: String = UUID.randomUUID().toString()) : NavigationCommand

abstract class DialogCommand(
    val dialogProvider: @Composable (onClose: () -> Unit) -> MaterialDialogState
) : NavigationCommand