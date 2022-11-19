package f.cking.software.ui.profiledetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.common.ClickableField
import f.cking.software.orNull
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.ScreenNavigationCommands.OpenDatePickerDialog
import f.cking.software.ui.ScreenNavigationCommands.OpenTimePickerDialog
import f.cking.software.ui.profiledetails.ProfileDetailsViewModel.UiFilterState
import f.cking.software.ui.selectfiltertype.FilterType
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

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
                Text(text = "Radar profile")
            },
            actions = {
                if (viewModel.profileId.isPresent) {
                    IconButton(onClick = { viewModel.onRemoveClick() }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = { viewModel.onSaveClick() }) {
                    Icon(imageVector = Icons.Filled.Done, contentDescription = "Save", tint = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        )
    }

    @Composable
    private fun Content(viewModel: ProfileDetailsViewModel) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item { Header(viewModel) }

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
    private fun Header(viewModel: ProfileDetailsViewModel) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            TextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                placeholder = { Text(text = "Name") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                placeholder = { Text(text = "Description (optional)") })
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Is active")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = viewModel.isActive, onCheckedChange = { viewModel.isActive = it })
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
                        viewModel.filter = Optional.of(getFilterByType(type))
                    })
                },
                content = {
                    Text(text = "Create filter")
                }
            )
        }
    }

    @Composable
    private fun Filter(
        filterState: UiFilterState,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        when (filterState) {
            is UiFilterState.All -> FilterAll(filterState, viewModel, onDeleteClick)
            is UiFilterState.Any -> FilterAny(filterState, viewModel, onDeleteClick)
            is UiFilterState.Not -> FilterNot(filterState, viewModel, onDeleteClick)
            is UiFilterState.Name -> FilterName(filterState, onDeleteClick)
            is UiFilterState.Address -> FilterAddress(viewModel, filterState, onDeleteClick)
            is UiFilterState.AppleAirdropContact -> FilterAirdropContact(viewModel, filterState, onDeleteClick)
            is UiFilterState.IsFavorite -> FilterIsFavorite(filterState, onDeleteClick)
            is UiFilterState.Manufacturer -> FilterManufacturer(viewModel, filterState, onDeleteClick)
            is UiFilterState.MinLostTime -> FilterMinLostPeriod(viewModel, filterState, onDeleteClick)
            is UiFilterState.LastDetectionInterval -> FilterLastDetectionInterval(viewModel, filterState, onDeleteClick)
            is UiFilterState.FirstDetectionInterval -> FilterFirstDetectionInterval(
                viewModel,
                filterState,
                onDeleteClick
            )
            else -> FilterUnknown(filterState, onDeleteClick)
        }
    }

    @Composable
    private fun FilterUnknown(filter: UiFilterState, onDeleteClick: (child: UiFilterState) -> Unit) {
        FilterBase(
            title = "Unknown filter",
            color = colorResource(R.color.filter_unknown),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Text(text = "Current filter is not supported by your app version")
        }
    }

    @Composable
    private fun FilterName(
        filter: UiFilterState.Name,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Name",
            color = colorResource(R.color.filter_name),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                TextField(value = filter.name, singleLine = true, onValueChange = {
                    filter.name = it
                }, placeholder = { Text(text = "Device name") })
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
    private fun FilterAirdropContact(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.AppleAirdropContact,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Airdrop contact",
            color = colorResource(R.color.filter_airdrop_contact),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                TextField(value = filter.contactString, singleLine = true, onValueChange = {
                    filter.contactString = it.lowercase()
                }, placeholder = { Text(text = "email/phone") })

                val text = filter.minLostTime.orNull()?.format(DateTimeFormatter.ofPattern("HH:mm"))
                val defaultTime = filter.minLostTime.orNull() ?: LocalTime.of(1, 0)
                val timeDialog = OpenTimePickerDialog(defaultTime) { time ->
                    filter.minLostTime = Optional.of(time)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    ClickableField(text = text, placeholder = "Min lost period") {
                        viewModel.router.navigate(timeDialog)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ClearIcon {
                        filter.minLostTime = Optional.empty()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Airdrop cannot be linked to a specific device because Apple changes BLE addresses every 15 minutes. To select the min lost period you should use this field.",
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }

    @Composable
    private fun FilterAddress(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.Address,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Address",
            color = colorResource(R.color.filter_address),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Row {
                TextField(value = filter.address, singleLine = true, onValueChange = {
                    filter.address = it.uppercase()
                }, placeholder = { Text(text = "00:00:00:00:00:00") })
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectDeviceScreen { device ->
                        filter.address = device.address
                    })
                }) {
                    Icon(imageVector = Icons.Filled.List, contentDescription = "Select device", tint = Color.Black)
                }
            }
        }
    }

    @Composable
    private fun FilterManufacturer(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.Manufacturer,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Manufacturer",
            color = colorResource(R.color.filter_manufacturer),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            val name: String? = filter.manufacturer.orNull()?.name
            val placeholder: String? = if (name == null) "Select" else null

            ClickableField(text = name, placeholder = placeholder) {
                viewModel.router.navigate(ScreenNavigationCommands.OpenSelectManufacturerScreen { manufacturer ->
                    filter.manufacturer = Optional.of(manufacturer)
                })
            }
        }
    }

    @Composable
    private fun FilterMinLostPeriod(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.MinLostTime,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Min lost period",
            color = colorResource(R.color.filter_lost_time),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            val defaultTime = filter.minLostTime.orNull() ?: LocalTime.of(1, 0)
            val timeDialog = OpenTimePickerDialog(defaultTime) { time ->
                filter.minLostTime = Optional.of(time)
            }

            val text = filter.minLostTime.orNull()?.format(DateTimeFormatter.ofPattern("HH:mm"))

            ClickableField(text = text, placeholder = "Chose time") {
                viewModel.router.navigate(timeDialog)
            }
        }
    }

    @Composable
    private fun FilterFirstDetectionInterval(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.FirstDetectionInterval,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "First detection interval",
            color = colorResource(R.color.filter_first_seen),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            TimeInterval(viewModel = viewModel, filter = filter)
        }
    }

    @Composable
    private fun FilterLastDetectionInterval(
        viewModel: ProfileDetailsViewModel,
        filter: UiFilterState.LastDetectionInterval,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Last detection interval",
            color = colorResource(R.color.filter_last_seen),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            TimeInterval(viewModel = viewModel, filter = filter)
        }
    }

    @Composable
    private fun TimeInterval(viewModel: ProfileDetailsViewModel, filter: UiFilterState.Interval) {

        val dateFormat = "dd MMM yyyy"
        val timeFormat = "HH:mm"

        val fromDateStr: String? = filter.fromDate.orNull()?.format(DateTimeFormatter.ofPattern(dateFormat))
        val fromTimeStr: String? = filter.fromTime.orNull()?.format(DateTimeFormatter.ofPattern(timeFormat))
        val toDateStr: String? = filter.toDate.orNull()?.format(DateTimeFormatter.ofPattern(dateFormat))
        val toTimeStr: String? = filter.toTime.orNull()?.format(DateTimeFormatter.ofPattern(timeFormat))

        val fromDateDialog = OpenDatePickerDialog(filter.fromDate.orNull() ?: LocalDate.now()) { date ->
            filter.fromDate = Optional.of(date)
        }
        val fromTimeDialog = OpenTimePickerDialog(filter.fromTime.orNull() ?: LocalTime.now()) { date ->
            filter.fromTime = Optional.of(date)
        }
        val toDateDialog = OpenDatePickerDialog(filter.toDate.orNull() ?: LocalDate.now()) { date ->
            filter.toDate = Optional.of(date)
        }
        val toTimeDialog = OpenTimePickerDialog(filter.toTime.orNull() ?: LocalTime.now()) { date ->
            filter.toTime = Optional.of(date)
        }

        val dateWidth = 150.dp
        val timeWidth = 150.dp
        val router = viewModel.router
        Column {
            Row {
                ClickableField(
                    modifier = Modifier.width(dateWidth),
                    text = fromDateStr,
                    placeholder = "From date"
                ) { router.navigate(fromDateDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.width(timeWidth),
                    text = fromTimeStr,
                    placeholder = "From time"
                ) { router.navigate(fromTimeDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClearIcon {
                    filter.fromDate = Optional.empty()
                    filter.fromTime = Optional.empty()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                ClickableField(
                    modifier = Modifier.width(dateWidth),
                    text = toDateStr,
                    placeholder = "To date"
                ) { router.navigate(toDateDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.width(timeWidth),
                    text = toTimeStr,
                    placeholder = "To time"
                ) { router.navigate(toTimeDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClearIcon {
                    filter.toDate = Optional.empty()
                    filter.toDate = Optional.empty()
                }
            }
        }
    }

    @Composable
    private fun ClearIcon(action: () -> Unit) {
        IconButton(onClick = action) { Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear") }
    }

    @Composable
    private fun FilterIsFavorite(
        filter: UiFilterState.IsFavorite,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = "Is favorite",
            color = colorResource(R.color.filter_is_favorite),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
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
        filter: UiFilterState.All,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterGroup(
            title = "All",
            color = colorResource(R.color.filter_all),
            addText = "Add",
            addClick = {
                viewModel.router.navigate(ScreenNavigationCommands.OpenSelectFilterTypeScreen { type ->
                    filter.filters = filter.filters + listOf(getFilterByType(type))
                })
            },
            onDeleteClick = { onDeleteClick.invoke(filter) }
        ) {
            filter.filters.forEach {
                Filter(filterState = it, viewModel = viewModel, onDeleteClick = filter::delete)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun FilterAny(
        filter: UiFilterState.Any,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterGroup(
            title = "Any",
            color = colorResource(R.color.filter_any),
            addText = "Add",
            addClick = {
                viewModel.router.navigate(ScreenNavigationCommands.OpenSelectFilterTypeScreen { type ->
                    filter.filters = filter.filters + listOf(getFilterByType(type))
                })
            },
            onDeleteClick = { onDeleteClick.invoke(filter) }
        ) {
            filter.filters.forEach {
                Filter(filterState = it, viewModel = viewModel, onDeleteClick = filter::delete)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun FilterNot(
        filter: UiFilterState.Not,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        val buttonText = if (filter.filter.isPresent) "Change" else "Set"
        FilterGroup(
            title = "Not",
            color = colorResource(R.color.filter_not),
            addText = buttonText,
            addClick = {
                viewModel.router.navigate(ScreenNavigationCommands.OpenSelectFilterTypeScreen { type ->
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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FilterBase(
        title: String,
        color: Color,
        onDeleteButtonClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Column(
            Modifier
                .padding(horizontal = 4.dp)
                .border(border = BorderStroke(2.dp, color), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Chip(
                onClick = onDeleteButtonClick,
                colors = ChipDefaults.chipColors(
                    backgroundColor = color,
                    contentColor = Color.Black,
                    leadingIconContentColor = Color.Black
                ),
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(24.dp))
                }
            ) {
                Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            Column {
                content.invoke()
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = addClick) {
                    Text(text = addText)
                }
            }
        }
    }

    private fun getFilterByType(type: FilterType): UiFilterState {
        return when (type) {
            FilterType.NAME -> UiFilterState.Name()
            FilterType.ADDRESS -> UiFilterState.Address()
            FilterType.BY_LAST_DETECTION -> UiFilterState.LastDetectionInterval()
            FilterType.BY_FIRST_DETECTION -> UiFilterState.FirstDetectionInterval()
            FilterType.BY_IS_FAVORITE -> UiFilterState.IsFavorite()
            FilterType.BY_MANUFACTURER -> UiFilterState.Manufacturer()
            FilterType.BY_LOGIC_ALL -> UiFilterState.All()
            FilterType.BY_LOGIC_ANY -> UiFilterState.Any()
            FilterType.BY_LOGIC_NOT -> UiFilterState.Not()
            FilterType.BY_MIN_DETECTION_TIME -> UiFilterState.MinLostTime()
            FilterType.AIRDROP_CONTACT -> UiFilterState.AppleAirdropContact()
        }
    }
}