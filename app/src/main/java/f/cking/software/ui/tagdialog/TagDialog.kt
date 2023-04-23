package f.cking.software.ui.tagdialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.common.TagChip
import f.cking.software.data.repo.TagsRepository
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getKoin

object TagDialog {

    @Composable
    fun rememberDialog(
        onSelected: (String) -> Unit,
    ): MaterialDialogState {
        val dialog = rememberMaterialDialogState()
        val inputState = remember { mutableStateOf("") }
        MaterialDialog(
            dialogState = dialog,
            buttons = {
                negativeButton("Cancel") { dialog.hide() }
            },
            onCloseRequest = { dialog.hide() },
        ) {
            Column(modifier = Modifier.padding(8.dp).fillMaxHeight()) {
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

            FlowRow(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
            ) {
                tags.value.forEach { name ->
                    TagChip(tagName = name, tagIcon = Icons.Filled.Delete) { onTagSelected.invoke(name) }
                }
            }
        }
    }
}