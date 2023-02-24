package f.cking.software.ui.profiledetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.common.ClickableField
import f.cking.software.dateTimeFormat
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
                        Filter(filterState = filter, viewModel = viewModel, onDeleteClick = { viewModel.filter = null })
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
                placeholder = { Text(text = stringResource(R.string.name)) })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                placeholder = { Text(text = stringResource(R.string.description)) })
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onIsActiveClick() },
                verticalAlignment = Alignment.CenterVertically
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
                        viewModel.filter = getFilterByType(type)
                    })
                },
                content = {
                    Text(text = stringResource(R.string.add_filter))
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
            is UiFilterState.FirstDetectionInterval -> FilterFirstDetectionInterval(viewModel, filterState, onDeleteClick)
            is UiFilterState.IsFollowing -> FilterIsFollowing(filterState, viewModel, onDeleteClick)
            is UiFilterState.Unknown, is UiFilterState.Interval -> FilterUnknown(filterState, onDeleteClick)
        }
    }

    @Composable
    private fun FilterIsFollowing(
        filter: UiFilterState.IsFollowing,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_device_is_following_me),
            color = colorResource(R.color.filter_is_following),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            val followingDuration = OpenTimePickerDialog(filter.followingDurationMs) { time ->
                filter.followingDurationMs = time
            }

            val followingInterval = OpenTimePickerDialog(filter.followingDetectionIntervalMs) { time ->
                filter.followingDetectionIntervalMs = time
            }

            val followingDurationText = filter.followingDurationMs.format(DateTimeFormatter.ofPattern("HH:mm"))
            val followingIntervalText = filter.followingDetectionIntervalMs.format(DateTimeFormatter.ofPattern("HH:mm"))

            Column {
                ClickableField(
                    text = followingDurationText,
                    placeholder = stringResource(R.string.time_placeholder),
                    label = stringResource(R.string.min_following_duration),
                ) {
                    viewModel.router.navigate(followingDuration)
                }
                Spacer(modifier = Modifier.height(8.dp))
                ClickableField(
                    text = followingIntervalText,
                    placeholder = stringResource(R.string.time_placeholder),
                    label = stringResource(R.string.min_interval_to_detect),
                ) {
                    viewModel.router.navigate(followingInterval)
                }
            }
        }
    }

    @Composable
    private fun FilterUnknown(filter: UiFilterState, onDeleteClick: (child: UiFilterState) -> Unit) {
        FilterBase(
            title = stringResource(R.string.filter_unknown),
            color = colorResource(R.color.filter_unknown),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Text(text = stringResource(R.string.filter_unknown_title))
        }
    }

    @Composable
    private fun FilterName(
        filter: UiFilterState.Name,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_name),
            color = colorResource(R.color.filter_name),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                TextField(
                    value = filter.name,
                    singleLine = true,
                    onValueChange = { filter.name = it },
                    placeholder = { Text(text = stringResource(R.string.placeholder_device_name)) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.ignore_case))
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
            title = stringResource(R.string.filter_apple_airdrop_contact),
            color = colorResource(R.color.filter_airdrop_contact),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                TextField(value = filter.contactString, singleLine = true, onValueChange = {
                    filter.contactString = it.lowercase()
                }, placeholder = { Text(text = stringResource(R.string.placeholder_airdrope_contact)) })

                val text = filter.minLostTime.orNull()?.format(DateTimeFormatter.ofPattern("HH:mm"))
                val defaultTime = filter.minLostTime.orNull() ?: LocalTime.of(1, 0)
                val timeDialog = OpenTimePickerDialog(defaultTime) { time ->
                    filter.minLostTime = Optional.of(time)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    ClickableField(
                        text = text,
                        label = stringResource(R.string.airdrop_min_lost_period),
                        placeholder = stringResource(R.string.time_placeholder)
                    ) {
                        viewModel.router.navigate(timeDialog)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ClearIcon {
                        filter.minLostTime = Optional.empty()
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.airdrop_issue_description),
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
            title = stringResource(R.string.filter_by_address),
            color = colorResource(R.color.filter_address),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Row {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = filter.address,
                    singleLine = true,
                    onValueChange = { filter.address = it.uppercase() },
                    placeholder = { Text(text = "00:00:00:00:00:00") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {
                    viewModel.router.navigate(ScreenNavigationCommands.OpenSelectDeviceScreen { device ->
                        filter.address = device.address
                    })
                }) {
                    Icon(imageVector = Icons.Filled.List, contentDescription = stringResource(R.string.select_device), tint = Color.Black)
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
            title = stringResource(R.string.filter_by_manufacturer),
            color = colorResource(R.color.filter_manufacturer),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            val name: String? = filter.manufacturer.orNull()?.name
            val label = if (name == null) stringResource(R.string.tap_to_select) else null

            ClickableField(
                text = name,
                placeholder = stringResource(R.string.select),
                label = label,
            ) {
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
            title = stringResource(R.string.filter_by_min_lost_period),
            color = colorResource(R.color.filter_lost_time),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            val defaultTime = filter.minLostTime.orNull() ?: LocalTime.of(1, 0)
            val timeDialog = OpenTimePickerDialog(defaultTime) { time ->
                filter.minLostTime = Optional.of(time)
            }

            val text = filter.minLostTime.orNull()?.dateTimeFormat("HH:mm")

            ClickableField(text = text, placeholder = stringResource(R.string.chose_time), label = null) {
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
            title = stringResource(R.string.filter_by_first_detection_period),
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
            title = stringResource(R.string.filter_by_last_detection_period),
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

        val fromDateStr: String? = filter.fromDate.orNull()?.dateTimeFormat(dateFormat)
        val fromTimeStr: String? = filter.fromTime.orNull()?.dateTimeFormat(timeFormat)
        val toDateStr: String? = filter.toDate.orNull()?.dateTimeFormat(dateFormat)
        val toTimeStr: String? = filter.toTime.orNull()?.dateTimeFormat(timeFormat)

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

        val router = viewModel.router
        Column {
            Row {
                val fromDatePlaceholder = stringResource(R.string.from_date)
                val fromTimePlaceholder = stringResource(R.string.from_time)
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = fromDateStr,
                    placeholder = fromDatePlaceholder,
                    label = if (fromDateStr != null) fromDatePlaceholder else null
                ) { router.navigate(fromDateDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = fromTimeStr,
                    placeholder = fromTimePlaceholder,
                    label = if (fromTimeStr != null) fromTimePlaceholder else null
                ) { router.navigate(fromTimeDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClearIcon {
                    filter.fromDate = Optional.empty()
                    filter.fromTime = Optional.empty()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                val toDatePlaceholder = stringResource(R.string.to_date)
                val toTimePlaceholder = stringResource(R.string.to_time)
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = toDateStr,
                    placeholder = toDatePlaceholder,
                    label = if (toDateStr != null) toDatePlaceholder else null,
                ) { router.navigate(toDateDialog) }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = toTimeStr,
                    placeholder = toTimePlaceholder,
                    label = if (toTimeStr != null) toTimePlaceholder else null,
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
    private fun ClearIcon(modifier: Modifier = Modifier, action: () -> Unit) {
        IconButton(modifier = modifier, onClick = action) {
            Icon(imageVector = Icons.Filled.Clear, contentDescription = stringResource(R.string.clear))
        }
    }

    @Composable
    private fun FilterIsFavorite(
        filter: UiFilterState.IsFavorite,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_is_favorite),
            color = colorResource(R.color.filter_is_favorite),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.is_favorite))
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
            title = stringResource(R.string.filter_all_of),
            color = colorResource(R.color.filter_all),
            addText = stringResource(R.string.add_filter),
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
            title = stringResource(R.string.filter_any_of),
            color = colorResource(R.color.filter_any),
            addText = stringResource(R.string.add_filter),
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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FilterNot(
        filter: UiFilterState.Not,
        viewModel: ProfileDetailsViewModel,
        onDeleteClick: (child: UiFilterState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_not),
            color = colorResource(R.color.filter_not),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) },
        ) {
            if (filter.filter.isPresent) {
                Filter(filter.filter.get(), viewModel, onDeleteClick = filter::delete)
            } else {
                Chip(
                    onClick = {
                        viewModel.router.navigate(ScreenNavigationCommands.OpenSelectFilterTypeScreen { type ->
                            filter.filter = Optional.of(getFilterByType(type))
                        })
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = colorResource(R.color.filter_not),
                        contentColor = Color.Black,
                        leadingIconContentColor = Color.Black
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_filter))
                    }
                ) {
                    Text(text = stringResource(R.string.select))
                }
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
        val minimize = remember { mutableStateOf(false) }
        val backgroundShape = RoundedCornerShape(8.dp)
        Column(
            Modifier
                .background(color, shape = backgroundShape)
                .border(width = 1.dp, color = Color.Black, backgroundShape)
                .clip(backgroundShape)
        ) {
            Box(Modifier.clickable { minimize.value = !minimize.value }) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    val icon = if (!minimize.value) painterResource(R.drawable.ic_drop_down) else painterResource(R.drawable.ic_drop_up)
                    Icon(
                        painter = icon,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDeleteButtonClick) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black,
                        )
                    }
                }
            }
            if (!minimize.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    content.invoke()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
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
                Chip(
                    onClick = addClick,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = color,
                        contentColor = Color.Black,
                        leadingIconContentColor = Color.Black
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = addText)
                    }
                ) {
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
            FilterType.IS_FOLLOWING -> UiFilterState.IsFollowing()
        }
    }
}