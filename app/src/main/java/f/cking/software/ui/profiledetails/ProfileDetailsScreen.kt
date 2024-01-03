package f.cking.software.ui.profiledetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.ui.filter.FilterScreen
import f.cking.software.ui.filter.FilterUiState
import f.cking.software.ui.filter.SelectFilterTypeScreen
import f.cking.software.utils.graphic.GlassSystemNavbar
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.SystemNavbarSpacer
import f.cking.software.utils.graphic.ThemedDialog
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
object ProfileDetailsScreen {

    @Composable
    fun Screen(profileId: Int?, template: FilterUiState?, key: String) {
        val viewModel: ProfileDetailsViewModel = koinViewModel(key = key) { parametersOf(profileId, template) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize(),
            topBar = {
                AppBar(viewModel, scrollBehavior)
            },
            content = {
                GlassSystemNavbar(Modifier.padding(top = it.calculateTopPadding())) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Content(viewModel)
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppBar(viewModel: ProfileDetailsViewModel, scrollBehavior: TopAppBarScrollBehavior) {

        val discardChangesDialog = rememberMaterialDialogState()
        ThemedDialog(
            dialogState = discardChangesDialog,
            buttons = {
                negativeButton(
                    stringResource(R.string.stay),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { discardChangesDialog.hide() }
                positiveButton(
                    stringResource(R.string.discard_changes),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) {
                    discardChangesDialog.hide()
                    viewModel.back()
                }
            }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.discard_changes_dialog_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        val deleteDialog = rememberMaterialDialogState()
        ThemedDialog(
            dialogState = deleteDialog,
            buttons = {
                negativeButton(stringResource(R.string.cancel), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) { deleteDialog.hide() }
                positiveButton(stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    deleteDialog.hide()
                    viewModel.onRemoveClick()
                }
            }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.delete_profile_dialog_title, viewModel.name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            title = {
                Text(text = stringResource(R.string.radar_profile_title))
            },
            actions = {
                if (viewModel.profileId != null) {
                    IconButton(onClick = { deleteDialog.show() }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = { viewModel.onSaveClick() }) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(R.string.save),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (viewModel.checkUnsavedChanges()) {
                        discardChangesDialog.show()
                    } else {
                        viewModel.back()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    @Composable
    private fun Content(viewModel: ProfileDetailsViewModel) {
        Header(viewModel)

        val filter = viewModel.filter
        if (filter != null) {
            Box(modifier = Modifier.padding(16.dp)) {
                FilterScreen.Filter(filterState = filter, router = viewModel.router, onDeleteClick = { viewModel.filter = null })
            }
        } else {
            CreateFilter(viewModel = viewModel)
        }

        SystemNavbarSpacer()
    }

    @Composable
    private fun Header(viewModel: ProfileDetailsViewModel) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                placeholder = { Text(text = stringResource(R.string.name)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                placeholder = { Text(text = stringResource(R.string.description)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoundedBox {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onIsActiveClick() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(modifier = Modifier.weight(1f), text = stringResource(R.string.is_active))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = viewModel.isActive, onCheckedChange = { viewModel.onIsActiveClick() })
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }

    @Composable
    private fun CreateFilter(viewModel: ProfileDetailsViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {

            val selectFilterDialog = rememberMaterialDialogState()
            SelectFilterTypeScreen.Dialog(selectFilterDialog) { filter ->
                viewModel.filter = filter
            }

            Button(
                onClick = {
                    selectFilterDialog.show()
                },
                content = {
                    Text(text = stringResource(R.string.add_filter), color = MaterialTheme.colorScheme.onPrimary)
                }
            )
        }
    }
}