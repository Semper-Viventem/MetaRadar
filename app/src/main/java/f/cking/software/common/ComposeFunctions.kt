package f.cking.software.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.domain.model.DeviceData
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun rememberDateDialog(
    initialDate: LocalDate = LocalDate.now(),
    dateResult: (date: LocalDate) -> Unit,
    onDialogClosed: () -> Unit,
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok") { onDialogClosed.invoke() }
            negativeButton("Cancel") { onDialogClosed.invoke() }
        },
        onCloseRequest = {
            it.hide()
            onDialogClosed.invoke()
        },
    ) {
        datepicker(initialDate = initialDate) { localDate ->
            dateResult.invoke(localDate)
        }
    }
    return dialogState
}

@Composable
fun rememberTimeDialog(
    initialTime: LocalTime = LocalTime.now(),
    dateResult: (date: LocalTime) -> Unit,
    onDialogClosed: () -> Unit,
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok") { onDialogClosed.invoke() }
            negativeButton("Cancel") { onDialogClosed.invoke() }
        },
        onCloseRequest = {
            it.hide()
            onDialogClosed.invoke()
        },
    ) {
        timepicker(is24HourClock = true, initialTime = initialTime) { localDate ->
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

@Composable
fun DeviceListItem(
    device: DeviceData,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick.invoke() },
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row() {
                    Text(text = device.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                device.manufacturerInfo?.name?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "lifetime: ${device.firstDetectionPeriod()} | last update: ${device.lastDetectionPeriod()} ago",
                    fontWeight = FontWeight.Light
                )
            }
            if (device.favorite) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorite")
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}