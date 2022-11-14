package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Navigator(
    root: AddToStackCommand?
) {

    var stack: List<@Composable () -> Unit> by mutableStateOf(emptyList())

    init {
        root?.let { handle(it) }
    }

    fun handle(command: NavigationCommand) {
        when (command) {
            is BackCommand -> handleBack()
            is AddToStackCommand -> handleAddToStackCommand(command)
        }
    }

    private fun handleAddToStackCommand(command: AddToStackCommand) {
        stack = stack + listOf(command.screenFunction)
    }

    private fun handleBack() {
        stack = stack.dropLast(1)
    }
}