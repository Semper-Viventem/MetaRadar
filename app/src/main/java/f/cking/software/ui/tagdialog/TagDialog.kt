package f.cking.software.ui.tagdialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.data.repo.TagsRepository
import f.cking.software.utils.graphic.TagChip
import f.cking.software.utils.graphic.ThemedDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getKoin

object TagDialog {

    @Composable
    fun rememberDialog(
        onSelected: (String) -> Unit,
    ): MaterialDialogState {
        val dialog = rememberMaterialDialogState()
        val inputState = remember { mutableStateOf("") }
        ThemedDialog(
            dialogState = dialog,
            buttons = {
                negativeButton("Cancel", textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) { dialog.hide() }
            },
            onCloseRequest = { dialog.hide() },
        ) {
            Column(modifier = Modifier
                .padding(8.dp)
                .height(300.dp)) {
                TextField(
                    value = inputState.value,
                    onValueChange = { inputState.value = it },
                    placeholder = { Text(text = stringResource(R.string.tag_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                TagsList(prefix = inputState.value, onTagSelected = {
                    onSelected.invoke(it)
                    inputState.value = ""
                    dialog.hide()
                })
            }
        }
        return dialog
    }

    @Composable
    private fun TagsList(
        prefix: String,
        onTagSelected: (String) -> Unit,
    ) {
        val repository: TagsRepository = getKoin().get()
        val scope = rememberCoroutineScope()
        val tags = remember { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(key1 = prefix, block = {
            scope.launch {
                tags.value = repository.getAll().filter { prefix.isBlank() || it.startsWith(prefix) }
            }
        })

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onTagSelected.invoke(prefix.trim()) },
                enabled = prefix.isNotBlank() && tags.value.none { it == prefix.trim() }
            ) {
                Text(text = stringResource(id = R.string.create_new_tag))
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        mainAxisSpacing = 8.dp,
                    ) {
                        tags.value.forEach { name ->
                            TagChip(tagName = name) { onTagSelected.invoke(name) }
                        }
                    }
                }
            }
        }
    }
}