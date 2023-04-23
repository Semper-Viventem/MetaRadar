package f.cking.software.ui.tagdialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
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
            onCloseRequest = { dialog.hide() }
        ) {
            TextField(
                value = inputState.value,
                onValueChange = { inputState.value = it },
                placeholder = { Text(text = stringResource(R.string.tag_name)) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            TagsList(prefix = inputState.value, onTagSelected = {
                onSelected.invoke(it)
                dialog.hide()
            })
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

        LazyColumn {
            scope.launch {
                val tags = repository.getAll().filter { prefix.isBlank() || it.startsWith(prefix) }
                item {
                    Button(
                        onClick = { onTagSelected.invoke(prefix.trim()) },
                        enabled = prefix.isNotBlank() && tags.none { it == prefix.trim() }
                    ) {
                        Text(text = stringResource(id = R.string.create_new_tag))
                    }
                }
                tags.forEach {
                    item {
                        Text(
                            text = it,
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { onTagSelected.invoke(it) }
                        )
                    }
                }
            }
        }
    }
}