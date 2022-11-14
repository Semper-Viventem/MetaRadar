package f.cking.software.ui.profiledetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.selectfiltertype.FilterType
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.*

object ProfileDetailsScreen {

    @Composable
    fun Screen(profileId: Int?) {
        val viewModel: ProfileDetailsViewModel = koinViewModel(parameters = {
            parametersOf(Optional.ofNullable(profileId))
        })
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
                Text(text = "Radar profile")
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }

    @Composable
    private fun Content(viewModel: ProfileDetailsViewModel) {
        LazyColumn(Modifier.fillMaxWidth()) {
            val filter = viewModel.filter
            if (filter.isPresent) {
                item {
                    Filter(
                        filterState = filter.get(),
                        viewModel = viewModel,
                        onDeleteClick = { viewModel.filter = Optional.empty() }
                    )
                }
            } else {
                item { CreateFilter(viewModel = viewModel) }
            }
        }
    }

    @Composable
    private fun CreateFilter(viewModel: ProfileDetailsViewModel) {
        Button(onClick = {
            viewModel.router.navigate(ScreenNavigationCommands.OpenSelectTypeScreen { type ->
                viewModel.filter = Optional.of(getFilterByType(type))
            })
        }) {
            Text(text = "Create filter")
        }
    }

    @Composable
    private fun Filter(
        filterState: ProfileDetailsViewModel.UiFilterState,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        when (filterState) {
            is ProfileDetailsViewModel.UiFilterState.All -> FilterAll(filterState, viewModel, onDeleteClick)
            is ProfileDetailsViewModel.UiFilterState.Any -> FilterAny(filterState, viewModel, onDeleteClick)
            is ProfileDetailsViewModel.UiFilterState.Not -> FilterNot(filterState, viewModel, onDeleteClick)
            is ProfileDetailsViewModel.UiFilterState.Name -> FilterName(filterState, onDeleteClick)
            is ProfileDetailsViewModel.UiFilterState.Address -> FilterAddress(filterState, onDeleteClick)
            is ProfileDetailsViewModel.UiFilterState.IsFavorite -> FilterIsFavorite(filterState, onDeleteClick)
            else -> {
                // do nothing
            }
        }
    }

    @Composable
    private fun FilterName(
        filter: ProfileDetailsViewModel.UiFilterState.Name,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        FilterBase(title = "Name", color = Color.Red, onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            Column {
                TextField(value = filter.name, onValueChange = {
                    filter.name = it
                })
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Ignore case")
                    Checkbox(checked = filter.ignoreCase, onCheckedChange = {
                        filter.ignoreCase = it
                    })
                }
            }
        }
    }

    @Composable
    private fun FilterAddress(
        filter: ProfileDetailsViewModel.UiFilterState.Address,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        FilterBase(title = "Address", color = Color.Red, onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            Column {
                TextField(value = filter.address, onValueChange = {
                    filter.address = it
                })
            }
        }
    }

    @Composable
    private fun FilterIsFavorite(
        filter: ProfileDetailsViewModel.UiFilterState.IsFavorite,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        FilterBase(title = "Is favorite", color = Color.Red, onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Is favorite")
                Checkbox(checked = filter.favorite, onCheckedChange = {
                    filter.favorite = it
                })
            }
        }
    }

    @Composable
    private fun FilterAll(
        filter: ProfileDetailsViewModel.UiFilterState.All,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            FilterGroup(
                title = "All",
                color = Color.Blue,
                addText = "Add",
                addClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectTypeScreen { type ->
                        filter.filters = filter.filters + listOf(getFilterByType(type))
                    })
                },
                onDeleteClick = { onDeleteClick.invoke(filter) }
            ) {
                filter.filters.forEach {
                    Filter(filterState = it, viewModel = viewModel, onDeleteClick = filter::delete)
                }
            }
        }
    }

    @Composable
    private fun FilterAny(
        filter: ProfileDetailsViewModel.UiFilterState.Any,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            FilterGroup(
                title = "Any",
                color = Color.Green,
                addText = "Add",
                addClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectTypeScreen { type ->
                        filter.filters = filter.filters + listOf(getFilterByType(type))
                    })
                },
                onDeleteClick = { onDeleteClick.invoke(filter) }
            ) {
                filter.filters.forEach {
                    Filter(filterState = it, viewModel = viewModel, onDeleteClick = filter::delete)
                }
            }
        }
    }

    @Composable
    private fun FilterNot(
        filter: ProfileDetailsViewModel.UiFilterState.Not,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: ProfileDetailsViewModel.UiFilterState) -> Unit,
    ) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            val buttonText = if (filter.filter.isPresent) "Change" else "Set"
            FilterGroup(
                title = "Not",
                color = Color.Black,
                addText = buttonText,
                addClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectTypeScreen { type ->
                        filter.filter = Optional.of(getFilterByType(type))
                    })
                },
                onDeleteClick = { onDeleteClick.invoke(filter) }
            ) {
                if (filter.filter.isPresent) {
                    Filter(filter.filter.get(), viewModel, onDeleteClick = filter::delete)
                }
            }
        }
    }

    @Composable
    private fun FilterBase(
        title: String,
        color: Color,
        onDeleteButtonClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteButtonClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            content.invoke()
        }
    }

    @Composable
    private fun FilterGroup(
        title: String,
        color: Color,
        addText: String,
        addClick: () -> Unit,
        onDeleteClick: () -> Unit,
        content: @Composable () -> Unit
    ) {
        FilterBase(title = title, color = color, onDeleteClick) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    content.invoke()
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = addClick) {
                        Text(text = addText)
                    }
                }
            }
        }
    }

    private fun getFilterByType(type: FilterType): ProfileDetailsViewModel.UiFilterState {
        return when (type) {
            FilterType.NAME -> ProfileDetailsViewModel.UiFilterState.Name()
            FilterType.ADDRESS -> ProfileDetailsViewModel.UiFilterState.Address()
            FilterType.BY_LAST_DETECTION -> ProfileDetailsViewModel.UiFilterState.LastDetectionInterval()
            FilterType.BY_FIRST_DETECTION -> ProfileDetailsViewModel.UiFilterState.FirstDetectionInterval()
            FilterType.BY_IS_FAVORITE -> ProfileDetailsViewModel.UiFilterState.IsFavorite()
            FilterType.BY_MANUFACTURER -> ProfileDetailsViewModel.UiFilterState.Manufacturer()
            FilterType.BY_LOGIC_ALL -> ProfileDetailsViewModel.UiFilterState.All()
            FilterType.BY_LOGIC_ANY -> ProfileDetailsViewModel.UiFilterState.Any()
            FilterType.BY_LOGIC_NOT -> ProfileDetailsViewModel.UiFilterState.Not()
            FilterType.BY_MIN_DETECTION_TIME -> ProfileDetailsViewModel.UiFilterState.MinLostTime()
        }
    }
}