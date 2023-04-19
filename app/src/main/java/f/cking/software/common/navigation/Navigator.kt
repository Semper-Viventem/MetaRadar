package f.cking.software.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Navigator(
    root: AddToStackCommand?
) : Router {

    var stack: List<@Composable () -> Unit> by mutableStateOf(emptyList())


    init {
        root?.let { handle(it) }
    }

    override fun navigate(command: NavigationCommand) {
        handle(command)
    }

    fun handle(command: NavigationCommand) {
        when (command) {
            is BackCommand -> handleBack()
            is AddToStackCommand -> handleAddToStackCommand { command.screenFunction(command.key, this)}
        }
    }

    private fun handleAddToStackCommand(screen: @Composable () -> Unit) {
        stack = stack + listOf(screen)
    }

    private fun handleBack() {
        stack = stack.dropLast(1)
    }
}