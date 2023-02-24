package f.cking.software.ui.profiledetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import f.cking.software.R
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.filter.FilterScreen
import org.koin.androidx.compose.koinViewModel

object ProfileDetailsScreen {

    @Composable
    fun Screen(profileId: Int?) {
        val viewModel: ProfileDetailsViewModel = koinViewModel()
        viewModel.setId(profileId)
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            topBar = {
                AppBar(viewModel)
            },
            content = {
                Box(modifier = Modifier.padding(it)) {
                    Content(viewModel)
                }
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: ProfileDetailsViewModel) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.radar_profile_title))
            },
            actions = {
                if (viewModel.profileId.isPresent) {
                    IconButton(onClick = { viewModel.onRemoveClick() }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = { viewModel.onSaveClick() }) {
                    Icon(imageVector = Icons.Filled.Done, contentDescription = stringResource(R.string.save), tint = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            }
        )
    }

    @Composable
    private fun Content(viewModel: ProfileDetailsViewModel) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item { Header(viewModel) }

            val filter = viewModel.filter
            if (filter != null) {
                item {
                    Box(modifier = Modifier.padding(8.dp)) {
                        FilterScreen.Filter(filterState = filter, router = viewModel.router, onDeleteClick = { viewModel.filter = null })
                    }
                }
            } else {
                item { CreateFilter(viewModel = viewModel) }
            }
        }
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

    @Composable
    private fun CreateFilter(viewModel: ProfileDetailsViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectFilterTypeScreen { type ->
                        viewModel.filter = FilterScreen.getFilterByType(type)
                    })
                },
                content = {
                    Text(text = stringResource(R.string.add_filter))
                }
            )
        }
    }
}