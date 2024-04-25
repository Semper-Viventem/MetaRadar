package f.cking.software.ui.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import f.cking.software.dateTimeFormat
import f.cking.software.dateTimeStringFormat
import f.cking.software.toLocalTime
import f.cking.software.toMilliseconds
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.tagdialog.TagDialog
import f.cking.software.utils.graphic.ClickableField
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.TagChip
import f.cking.software.utils.graphic.rememberDateDialog
import f.cking.software.utils.graphic.rememberTimeDialog
import f.cking.software.utils.navigation.Router
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object FilterScreen {

    @Composable
    fun Filter(
        filterState: FilterUiState,
        router: Router,
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
            is FilterUiState.DeviceLocation -> FilterDeviceLocation(filterState, router, onDeleteClick)
            is FilterUiState.UserLocation -> FilterUserLocation(filterState, router, onDeleteClick)
            is FilterUiState.Tag -> FilterTag(filterState, onDeleteClick)
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
            val followingDuration = rememberTimeDialog(filter.followingDurationMs.toLocalTime(ZoneId.of("GMT"))) { time ->
                filter.followingDurationMs = time.toMilliseconds()
            }

            val followingInterval = rememberTimeDialog(filter.followingDetectionIntervalMs.toLocalTime(ZoneId.of("GMT"))) { time ->
                filter.followingDetectionIntervalMs = time.toMilliseconds()
            }

            val followingDurationText = filter.followingDurationMs.dateTimeStringFormat("HH:mm", ZoneId.of("GMT"))
            val followingIntervalText = filter.followingDetectionIntervalMs.dateTimeStringFormat("HH:mm", ZoneId.of("GMT"))

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
    private fun FilterTag(
        filter: FilterUiState.Tag,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_tag),
            color = colorResource(R.color.filter_tag),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {

            val addTagDialog = TagDialog.rememberDialog {
                filter.tag = it
            }
            val tag = filter.tag

            if (tag == null) {
                SuggestionChip(
                    onClick = { addTagDialog.show() },
                    icon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    label = { Text(text = stringResource(R.string.select_tag)) }
                )
            } else {
                TagChip(tagName = tag, tagIcon = Icons.Filled.Delete) { filter.tag = null }
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

                val text = filter.minLostTime?.dateTimeStringFormat("HH:mm", ZoneId.of("GMT"))
                val defaultTime: Long = filter.minLostTime ?: TimeUnit.HOURS.toMillis(1)
                val timeDialog = rememberTimeDialog(defaultTime.toLocalTime(ZoneId.of("GMT"))) { time ->
                    filter.minLostTime = time.toMilliseconds()
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
                    ClearIcon { filter.minLostTime = null }
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
        router: Router,
        filter: FilterUiState.Address,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_address),
            color = colorResource(R.color.filter_address),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = filter.address,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.filter_by_address_disclaimer)) },
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
        router: Router,
        filter: FilterUiState.Manufacturer,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_by_manufacturer),
            color = colorResource(R.color.filter_manufacturer),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }) {
            val name: String? = filter.manufacturer?.name
            val label = if (name == null) stringResource(R.string.tap_to_select) else null

            ClickableField(
                text = name,
                placeholder = stringResource(R.string.select),
                label = label,
            ) {
                router.navigate(ScreenNavigationCommands.OpenSelectManufacturerScreen { manufacturer ->
                    filter.manufacturer = manufacturer
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
            val defaultTime = filter.minLostTime ?: TimeUnit.HOURS.toMillis(1)
            val timeDialog = rememberTimeDialog(defaultTime.toLocalTime(ZoneId.of("GMT"))) { time ->
                filter.minLostTime = time.toMilliseconds()
            }

            val text = filter.minLostTime?.dateTimeStringFormat("HH:mm", ZoneId.of("GMT"))

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

        val fromDateStr: String? = filter.fromDate?.dateTimeFormat(dateFormat)
        val fromTimeStr: String? = filter.fromTime?.dateTimeFormat(timeFormat)
        val toDateStr: String? = filter.toDate?.dateTimeFormat(dateFormat)
        val toTimeStr: String? = filter.toTime?.dateTimeFormat(timeFormat)

        val fromDateDialog = rememberDateDialog(filter.fromDate ?: LocalDate.now()) { date ->
            filter.fromDate = date
        }
        val fromTimeDialog = rememberTimeDialog(filter.fromTime ?: LocalTime.now()) { date ->
            filter.fromTime = date
        }
        val toDateDialog = rememberDateDialog(filter.toDate ?: LocalDate.now()) { date ->
            filter.toDate = date
        }
        val toTimeDialog = rememberTimeDialog(filter.toTime ?: LocalTime.now()) { date ->
            filter.toTime = date
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
                    filter.fromDate = null
                    filter.fromTime = null
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
                    filter.toDate = null
                    filter.toTime = null
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
        router: Router,
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
    private fun FilterDeviceLocation(
        filter: FilterUiState.DeviceLocation,
        router: Router,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_device_location),
            color = colorResource(R.color.filter_device_location),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                RoundedBox(modifier = Modifier.fillMaxWidth(), internalPaddings = 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                router.navigate(ScreenNavigationCommands.OpenSelectLocationScreen(
                                    initialLocationModel = filter.targetLocation,
                                    initialRadius = filter.radius,
                                ) { location, radiusMeters ->
                                    filter.targetLocation = location
                                    filter.radius = radiusMeters
                                })
                            }
                    ) {
                        val locationText = if (filter.targetLocation != null) {
                            stringResource(R.string.filter_location_has_data, filter.radius)
                        } else {
                            stringResource(R.string.filter_location_no_data)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Text(modifier = Modifier.weight(1f), text = locationText)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = locationText,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TimeInterval(filter = filter)
            }
        }
    }

    @Composable
    private fun FilterUserLocation(
        filter: FilterUiState.UserLocation,
        router: Router,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        FilterBase(
            title = stringResource(R.string.filter_user_location),
            color = colorResource(R.color.filter_user_location),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) }
        ) {
            Column {
                RoundedBox(modifier = Modifier.fillMaxWidth(), internalPaddings = 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                router.navigate(ScreenNavigationCommands.OpenSelectLocationScreen(
                                    initialLocationModel = filter.targetLocation,
                                    initialRadius = filter.radius,
                                ) { location, radiusMeters ->
                                    filter.targetLocation = location
                                    filter.radius = radiusMeters
                                })
                            }
                    ) {
                        val locationText = if (filter.targetLocation != null) {
                            stringResource(R.string.filter_location_has_data, filter.radius)
                        } else {
                            stringResource(R.string.filter_location_no_data)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Text(modifier = Modifier.weight(1f), text = locationText)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = locationText,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filter.defaultValueIfNoLocation = !filter.defaultValueIfNoLocation },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(modifier = Modifier.weight(1f), text = stringResource(R.string.filter_user_location_if_no_location))
                    Spacer(modifier = Modifier.width(4.dp))
                    Checkbox(
                        checked = filter.defaultValueIfNoLocation,
                        onCheckedChange = {
                            filter.defaultValueIfNoLocation = it
                        })
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }

    @Composable
    private fun FilterAny(
        filter: FilterUiState.Any,
        router: Router,
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

    @Composable
    private fun FilterNot(
        filter: FilterUiState.Not,
        router: Router,
        onDeleteClick: (child: FilterUiState) -> Unit,
    ) {
        val selectFilterDialog = rememberMaterialDialogState()
        SelectFilterTypeScreen.Dialog(selectFilterDialog) { newFilter ->
            filter.filter = newFilter
        }

        FilterBase(
            title = stringResource(R.string.filter_not),
            color = colorResource(R.color.filter_not),
            onDeleteButtonClick = { onDeleteClick.invoke(filter) },
        ) {
            if (filter.filter != null) {
                Filter(filter.filter!!, router = router, onDeleteClick = filter::delete)
            } else {
                SuggestionChip(
                    onClick = { selectFilterDialog.show() },
                    icon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_filter))
                    },
                    label = { Text(text = stringResource(R.string.select)) },
                )
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
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val icon = if (!minimize.value) painterResource(R.drawable.ic_drop_down) else painterResource(R.drawable.ic_drop_up)
                    Icon(
                        painter = icon,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDeleteButtonClick) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
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
                SuggestionChip(
                    onClick = addClick,
                    icon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = addText)
                    },
                    label = { Text(text = addText) },
                )
            }
        }
    }
}