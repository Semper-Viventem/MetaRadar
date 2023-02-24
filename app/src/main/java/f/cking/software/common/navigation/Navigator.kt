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
            is AddToStackCommand -> handleAddToStackCommand { command.screenFunction(command.key)}
            is DialogCommand -> handleAddToStackCommand {
                val dialog = command.dialogProvider.invoke {
                    handleBack()
                }
                dialog.show()
            }
        }
    }

    private fun handleAddToStackCommand(screen: @Composable () -> Unit) {
        stack = stack + listOf(screen)
    }

    private fun handleBack() {
        stack = stack.dropLast(1)
    }
}