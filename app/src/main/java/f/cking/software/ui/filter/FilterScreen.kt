package f.cking.software.ui.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.common.ClickableField
import f.cking.software.common.navigation.NavRouter
import f.cking.software.common.rememberDateDialog
import f.cking.software.common.rememberTimeDialog
import f.cking.software.dateTimeFormat
import f.cking.software.orNull
import f.cking.software.ui.ScreenNavigationCommands
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object FilterScreen {

    @Composable
    fun Filter(
        filterState: FilterUiState,
        router: NavRouter,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        when (filterState) {
            is FilterUiState.All -> FilterAll(filterState, router, onDeleteClick)
            is FilterUiState.Any -> FilterAny(filterState, router, onDeleteClick)
            is FilterUiState.Not -> FilterNot(filterState, router, onDeleteClick)
            is FilterUiState.Name -> FilterName(filterState, onDeleteClick)
            is FilterUiState.Address -> FilterAddress(router, filterState, onDeleteClick)
            is FilterUiState.AppleAirdropContact -> FilterAirdropContact(filterState, onDeleteClick)
            is FilterUiState.IsFavorite -> FilterIsFavorite(filterState, onDeleteClick)
            is FilterUiState.Manufacturer -> FilterManufacturer(router, filterState, onDeleteClick)
            is FilterUiState.MinLostTime -> FilterMinLostPeriod(filterState, onDeleteClick)
            is FilterUiState.LastDetectionInterval -> FilterLastDetectionInterval(filterState, onDeleteClick)
            is FilterUiState.FirstDetectionInterval -> FilterFirstDetectionInterval(filterState, onDeleteClick)
            is FilterUiState.IsFollowing -> FilterIsFollowing(filterState, onDeleteClick)
            is FilterUiState.Unknown, is FilterUiState.Interval -> FilterUnknown(filterState, onDeleteClick)
        }
    }

    @Composable
    private fun FilterIsFollowing(
        filter: FilterUiState.IsFollowing,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_device_is_following_me),
            color = colorResource(R.color.filter_is_following),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            val followingDuration = rememberTimeDialog(filter.followingDurationMs) { time ->
                filter.followingDurationMs = time
            }

            val followingInterval = rememberTimeDialog(filter.followingDetectionIntervalMs) { time ->
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
                    followingDuration.show()
                }
                Spacer(modifier = Modifier.height(8.dp))
                ClickableField(
                    text = followingIntervalText,
                    placeholder = stringResource(R.string.time_placeholder),
                    label = stringResource(R.string.min_interval_to_detect),
                ) {
                    followingInterval.show()
                }
            }
        }
    }

    @Composable
    private fun FilterUnknown(filter: FilterUiState, onDeleteClick: (child: FilterUiState) -> Unit) {
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
        filter: FilterUiState.Name,
        onDeleteClick: (child: FilterUiState) -> Unit,
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
        filter: FilterUiState.AppleAirdropContact,
        onDeleteClick: (child: FilterUiState) -> Unit,
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
                val timeDialog = rememberTimeDialog(defaultTime) { time ->
                    filter.minLostTime = Optional.of(time)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    ClickableField(
                        text = text,
                        label = stringResource(R.string.airdrop_min_lost_period),
                        placeholder = stringResource(R.string.time_placeholder)
                    ) {
                        timeDialog.show()
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
        router: NavRouter,
        filter: FilterUiState.Address,
        onDeleteClick: (child: FilterUiState) -> Unit,
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
                    router.navigate(ScreenNavigationCommands.OpenSelectDeviceScreen { device ->
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
        router: NavRouter,
        filter: FilterUiState.Manufacturer,
        onDeleteClick: (child: FilterUiState) -> Unit,
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
                router.navigate(ScreenNavigationCommands.OpenSelectManufacturerScreen { manufacturer ->
                    filter.manufacturer = Optional.of(manufacturer)
                })
            }
        }
    }

    @Composable
    private fun FilterMinLostPeriod(
        filter: FilterUiState.MinLostTime,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_min_lost_period),
            color = colorResource(R.color.filter_lost_time),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            val defaultTime = filter.minLostTime.orNull() ?: LocalTime.of(1, 0)
            val timeDialog = rememberTimeDialog(defaultTime) { time ->
                filter.minLostTime = Optional.of(time)
            }

            val text = filter.minLostTime.orNull()?.dateTimeFormat("HH:mm")

            ClickableField(text = text, placeholder = stringResource(R.string.chose_time), label = null) {
                timeDialog.show()
            }
        }
    }

    @Composable
    private fun FilterFirstDetectionInterval(
        filter: FilterUiState.FirstDetectionInterval,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_first_detection_period),
            color = colorResource(R.color.filter_first_seen),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            TimeInterval(filter)
        }
    }

    @Composable
    private fun FilterLastDetectionInterval(
        filter: FilterUiState.LastDetectionInterval,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_last_detection_period),
            color = colorResource(R.color.filter_last_seen),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            TimeInterval(filter)
        }
    }

    @Composable
    private fun TimeInterval(filter: FilterUiState.Interval) {

        val dateFormat = "dd MMM yyyy"
        val timeFormat = "HH:mm"

        val fromDateStr: String? = filter.fromDate.orNull()?.dateTimeFormat(dateFormat)
        val fromTimeStr: String? = filter.fromTime.orNull()?.dateTimeFormat(timeFormat)
        val toDateStr: String? = filter.toDate.orNull()?.dateTimeFormat(dateFormat)
        val toTimeStr: String? = filter.toTime.orNull()?.dateTimeFormat(timeFormat)

        val fromDateDialog = rememberDateDialog(filter.fromDate.orNull() ?: LocalDate.now()) { date ->
            filter.fromDate = Optional.of(date)
        }
        val fromTimeDialog = rememberTimeDialog(filter.fromTime.orNull() ?: LocalTime.now()) { date ->
            filter.fromTime = Optional.of(date)
        }
        val toDateDialog = rememberDateDialog(filter.toDate.orNull() ?: LocalDate.now()) { date ->
            filter.toDate = Optional.of(date)
        }
        val toTimeDialog = rememberTimeDialog(filter.toTime.orNull() ?: LocalTime.now()) { date ->
            filter.toTime = Optional.of(date)
        }

        Column {
            Row {
                val fromDatePlaceholder = stringResource(R.string.from_date)
                val fromTimePlaceholder = stringResource(R.string.from_time)
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = fromDateStr,
                    placeholder = fromDatePlaceholder,
                    label = if (fromDateStr != null) fromDatePlaceholder else null
                ) { fromDateDialog.show() }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = fromTimeStr,
                    placeholder = fromTimePlaceholder,
                    label = if (fromTimeStr != null) fromTimePlaceholder else null
                ) { fromTimeDialog.show() }
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
                ) { toDateDialog.show() }
                Spacer(modifier = Modifier.width(2.dp))
                ClickableField(
                    modifier = Modifier.weight(1f),
                    text = toTimeStr,
                    placeholder = toTimePlaceholder,
                    label = if (toTimeStr != null) toTimePlaceholder else null,
                ) { toTimeDialog.show() }
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
        filter: FilterUiState.IsFavorite,
        onDeleteClick: (child: FilterUiState) -> Unit,
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
        filter: FilterUiState.All,
        router: NavRouter,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        val selectFilterDialog = rememberMaterialDialogState()
        SelectFilterTypeScreen.Dialog(selectFilterDialog) { newFilter ->
            filter.filters = filter.filters + listOf(newFilter)
        }

        FilterGroup(
            title = stringResource(R.string.filter_all_of),
            color = colorResource(R.color.filter_all),
            addText = stringResource(R.string.add_filter),
            addClick = { selectFilterDialog.show() },
            onDeleteClick = { onDeleteClick.invoke(filter) }
        ) {
            filter.filters.forEach {
                Filter(filterState = it, router = router, onDeleteClick = filter::delete)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun FilterAny(
        filter: FilterUiState.Any,
        router: NavRouter,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        val selectFilterDialog = rememberMaterialDialogState()
        SelectFilterTypeScreen.Dialog(selectFilterDialog) { newFilter ->
            filter.filters = filter.filters + listOf(newFilter)
        }

        FilterGroup(
            title = stringResource(R.string.filter_any_of),
            color = colorResource(R.color.filter_any),
            addText = stringResource(R.string.add_filter),
            addClick = { selectFilterDialog.show() },
            onDeleteClick = { onDeleteClick.invoke(filter) }
        ) {
            filter.filters.forEach {
                Filter(filterState = it, router = router, onDeleteClick = filter::delete)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FilterNot(
        filter: FilterUiState.Not,
        router: NavRouter,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        val selectFilterDialog = rememberMaterialDialogState()
        SelectFilterTypeScreen.Dialog(selectFilterDialog) { newFilter ->
            filter.filter = Optional.of(newFilter)
        }

        FilterBase(
            title = stringResource(R.string.filter_not),
            color = colorResource(R.color.filter_not),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) },
        ) {
            if (filter.filter.isPresent) {
                Filter(filter.filter.get(), router = router, onDeleteClick = filter::delete)
            } else {
                Chip(
                    onClick = { selectFilterDialog.show() },
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
}