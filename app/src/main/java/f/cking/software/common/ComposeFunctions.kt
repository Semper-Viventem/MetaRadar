package f.cking.software.common

import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun rememberDateDialog(dateResult: (date: LocalDate) -> Unit): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        datepicker { localDate ->
            dateResult.invoke(localDate)
        }
    }
    return dialogState
}

@Composable
fun rememberTimeDialog(dateResult: (date: LocalTime) -> Unit): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        timepicker(is24HourClock = true) { localDate ->
            dateResult.invoke(localDate)
        }
    }
    return dialogState
}

@Composable
fun ClickableField(
    text: String?,
    placeholder: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused) {
                    onClick.invoke()
                    focusManager.clearFocus(true)
                }
            },
        value = text ?: "",
        onValueChange = {},
        readOnly = true,
        placeholder = { placeholder?.let { Text(text = it) } },
    )
}