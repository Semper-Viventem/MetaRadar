package f.cking.software.ui.devicedetails

import android.bluetooth.BluetoothGattCharacteristic
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.dpToPx
import f.cking.software.frameRate
import f.cking.software.ui.AsyncBatchProcessor
import f.cking.software.ui.map.MapView
import f.cking.software.ui.tagdialog.TagDialog
import f.cking.software.utils.graphic.DevicePairedIcon
import f.cking.software.utils.graphic.DeviceTypeIcon
import f.cking.software.utils.graphic.ExtendedAddressView
import f.cking.software.utils.graphic.GlassSystemNavbar
import f.cking.software.utils.graphic.ListItem
import f.cking.software.utils.graphic.RadarIcon
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.SignalData
import f.cking.software.utils.graphic.SystemNavbarSpacer
import f.cking.software.utils.graphic.TagChip
import f.cking.software.utils.graphic.ThemedDialog
import f.cking.software.utils.graphic.infoDialog
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import timber.log.Timber
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
object DeviceDetailsScreen {

    @Composable
    fun Screen(
        address: String,
        viewModel: DeviceDetailsViewModel = koinViewModel(key = address) { parametersOf(address) }
    ) {

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            topBar = {
                AppBar(viewModel = viewModel, scrollBehavior)
            },
            content = { padding ->
                GlassSystemNavbar(modifier = Modifier.fillMaxSize()) {
                    Content(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        viewModel = viewModel,
                    )
                }
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: DeviceDetailsViewModel, scrollBehavior: TopAppBarScrollBehavior) {
        val deviceData = viewModel.deviceState
        TopAppBar(
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = stringResource(R.string.device_details_title))
            },
            actions = {
                if (deviceData != null) {
                    IconButton(onClick = { viewModel.onFavoriteClick(deviceData) }) {
                        val iconId =
                            if (deviceData.favorite) R.drawable.ic_star else R.drawable.ic_star_outline
                        val text = if (deviceData.favorite) stringResource(R.string.is_favorite) else stringResource(R.string.is_not_favorite)
                        Icon(
                            imageVector = ImageVector.vectorResource(id = iconId),
                            contentDescription = text,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
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
    private fun Content(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
    ) {
        val deviceData = viewModel.deviceState
        if (deviceData == null) {
            Progress(modifier)
        } else {
            DeviceDetails(modifier = modifier, viewModel = viewModel, deviceData = deviceData)
        }
    }

    @Composable
    private fun Progress(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun DeviceDetails(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
        deviceData: DeviceData
    ) {
        var scrollEnabled by remember { mutableStateOf(true) }
        val isMoving = remember { mutableStateOf(false) }

        LaunchedEffect(isMoving.value) {
            scrollEnabled = !isMoving.value
        }

        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState(), scrollEnabled)
                .fillMaxSize(),
        ) {
            LocationHistory(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                deviceData = deviceData,
                viewModel = viewModel,
                isMoving = isMoving,
            )
            OnlineStatus(viewModel = viewModel, deviceData.isConnectable)
            Spacer(modifier = Modifier.height(16.dp))
            Tags(deviceData = deviceData, viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            DeviceContent(modifier = Modifier, deviceData = deviceData, viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            SystemNavbarSpacer()
        }
    }

    @Composable
    private fun OnlineStatus(
        viewModel: DeviceDetailsViewModel,
        isConnectable: Boolean,
    ) {
        viewModel.onlineStatusData?.let { onlineStatus ->
            Spacer(modifier = Modifier.height(16.dp))
            RoundedBox(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadarIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(modifier = Modifier, text = stringResource(id = R.string.device_is_online), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = stringResource(viewModel.connectionStatus.statusRes))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isConnectable) {
                        when (val status = viewModel.connectionStatus) {
                            is DeviceDetailsViewModel.ConnectionStatus.DISCONNECTED -> {
                                Button(onClick = { viewModel.establishConnection() }) {
                                    Text(text = stringResource(R.string.device_details_connect), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }

                            is DeviceDetailsViewModel.ConnectionStatus.CONNECTED -> {
                                Button(onClick = { viewModel.disconnect(status.gatt) }) {
                                    Text(text = stringResource(R.string.device_details_disconnect), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }

                            is DeviceDetailsViewModel.ConnectionStatus.CONNECTING, is DeviceDetailsViewModel.ConnectionStatus.DISCONNECTING -> {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    SignalData(rssi = onlineStatus.signalStrength, distance = onlineStatus.distance)
                }
            }
        }
    }

    @Composable
    private fun DeviceContent(
        modifier: Modifier = Modifier,
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        RoundedBox(
            modifier = modifier
                .fillMaxWidth(),
            internalPaddings = 0.dp,
        ) {
            SelectionContainer {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DeviceTypeIcon(modifier = Modifier.size(42.dp), device = deviceData, paddingDp = 4.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = deviceData.buildDisplayName(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        DevicePairedIcon(deviceData.isPaired, extended = true)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.device_details_name), fontWeight = FontWeight.Bold)
                    Text(text = deviceData.resolvedName ?: stringResource(R.string.not_applicable))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.device_details_address), fontWeight = FontWeight.Bold)
                    ExtendedAddressView(deviceData.extendedAddressInfo())
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.device_details_manufacturer), fontWeight = FontWeight.Bold)
                    Text(text = deviceData.resolvedManufacturerName ?: stringResource(R.string.not_applicable))
                    Spacer(modifier = Modifier.height(8.dp))

                    DeviceMetadataView(deviceData, viewModel)
                    Spacer(modifier = Modifier.height(8.dp))

                    Services(viewModel.services, viewModel)
                    Spacer(modifier = Modifier.height(8.dp))

                    RawData(viewModel.rawData)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Text(
                            text = stringResource(R.string.device_details_detect_count),
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = deviceData.detectCount.toString())
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.device_details_first_detection), fontWeight = FontWeight.Bold)
                    Text(text = deviceData.firstDetectionExactTime(LocalContext.current, formatStyle = FormatStyle.MEDIUM))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.device_details_last_detection), fontWeight = FontWeight.Bold)
                    Text(text = deviceData.lastDetectionExactTime(LocalContext.current, formatStyle = FormatStyle.MEDIUM))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun DeviceMetadataView(device: DeviceData, viewModel: DeviceDetailsViewModel) {
        val metadata = device.metadata
        ExpandableLine(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.device_details_metadata),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (viewModel.matadataIsFetching) {
                        CircularProgressIndicator()
                    } else if (device.isConnectable) {
                        TagChip(stringResource(R.string.analyse)) { viewModel.fetchDeviceServiceInfo(device) }
                    }
                }
            },
            isExpandable = !metadata.isNullOrEmpty(),
            content = {
                Column {
                    metadata?.deviceName?.let { Text(text = it) }
                    metadata?.manufacturerName?.let { Text(text = it) }
                    metadata?.modelNumber?.let { Text(text = it) }
                    metadata?.serialNumber?.let { Text(text = it) }
                    metadata?.batteryLevel?.let { Text(text = "$it %") }
                }
            }
        )
    }

    @Composable
    private fun Services(servicesUuids: Set<DeviceDetailsViewModel.ServiceData>, viewModel: DeviceDetailsViewModel) {
        ExpandableLine(pluralStringResource(R.plurals.services_discovered, servicesUuids.size, servicesUuids.size)) {
            servicesUuids.forEach { service ->
                ServiceDetails(service, viewModel)
            }
        }
    }

    @Composable
    private fun ServiceDetails(service: DeviceDetailsViewModel.ServiceData, viewModel: DeviceDetailsViewModel) {
        val serviceUuid = service.uuid
        val name = service.name
        ExpandableLine(
            title = {
                Column {
                    Text(
                        text = serviceUuid,
                        fontWeight = FontWeight.Bold,
                    )
                    if (name != null) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Light,
                        )
                    }
                }
            },
            isExpandable = service.characteristics.isNotEmpty()
        ) {
            Column(
                Modifier
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                service.characteristics.forEach { characteristic ->
                    CharacteristicDetails(characteristic, viewModel)
                }
            }
        }
    }

    @Composable
    private fun CharacteristicDetails(characteristic: DeviceDetailsViewModel.CharacteristicData, viewModel: DeviceDetailsViewModel) {
        val characteristicUuid = characteristic.uuid
        val isExpandable = characteristic.gatt.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
        val name = characteristic.name

        ExpandableLine(
            title = {
                Column {
                    Text(
                        text = characteristicUuid,
                        fontWeight = FontWeight.Bold,
                    )
                    if (name != null) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Light,
                        )
                    }
                }
            },
            isExpandable = isExpandable
        ) {
            val value = characteristic.value
            val valueHex = characteristic.valueHex
            if (value != null && valueHex != null) {
                Text(value)
                Text(valueHex)
            } else {
                TagChip(stringResource(R.string.read)) { viewModel.readCharacteristic(characteristic.gatt) }
            }
        }
    }

    @Composable
    private fun RawData(rawData: List<Pair<String, String>>) {
        ExpandableLine(pluralStringResource(R.plurals.device_details_raw_data, rawData.size, rawData.size)) {
            rawData.forEach { (key, value) ->
                Row {
                    Text(modifier = Modifier.padding(8.dp), text = "0x$key")
                    Spacer(Modifier.width(8.dp))
                    Text(modifier = Modifier.padding(8.dp), text = "0x$value")
                }
            }
        }
    }

    @Composable
    private fun ExpandableLine(title: String, isExpandable: Boolean = true, content: @Composable () -> Unit) {
        ExpandableLine(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                )
            },
            isExpandable = isExpandable,
            content = content
        )
    }

    @Composable
    private fun ExpandableLine(title: @Composable () -> Unit, isExpandable: Boolean = true, content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            var expanded by remember { mutableStateOf(false) }
            val rotation by animateFloatAsState(180f * if (expanded) 1 else 0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isExpandable) {
                            expanded = !expanded
                        }
                    }
                    .padding(vertical = 8.dp)
            ) {
                title.invoke()
                if (isExpandable) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        modifier = Modifier.rotate(rotation),
                        painter = painterResource(R.drawable.ic_drop_up),
                        contentDescription = null,
                    )
                }
            }

            AnimatedVisibility(expanded) {
                Column {
                    content.invoke()
                }
            }
        }
    }

    @Composable
    private fun Tags(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        RoundedBox(
            modifier = Modifier.fillMaxWidth(),
            internalPaddings = 0.dp
        ) {
            FlowRow(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
            ) {
                AddTag(viewModel = viewModel, deviceData = deviceData)
                deviceData.tags.forEach { tag ->
                    Tag(name = tag, viewModel = viewModel, deviceData = deviceData)
                }
            }
        }
    }

    @Composable
    private fun Tag(
        deviceData: DeviceData,
        name: String,
        viewModel: DeviceDetailsViewModel,
    ) {
        val dialogState = rememberMaterialDialogState()

        ThemedDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    dialogState.hide()
                    viewModel.onRemoveTagClick(deviceData, name)
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.delete_tag_title, name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        TagChip(tagName = name, tagIcon = Icons.Filled.Delete) { dialogState.show() }
    }

    @Composable
    private fun AddTag(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        val addTagDialog = TagDialog.rememberDialog {
            viewModel.onNewTagSelected(deviceData, it)
        }
        SuggestionChip(
            onClick = { addTagDialog.show() },
            icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            },
            label = { Text(text = stringResource(R.string.add_tag)) }
        )
    }

    @Composable
    private fun PointsStyle(
        viewModel: DeviceDetailsViewModel,
    ) {
        val dialog = rememberMaterialDialogState()
        ThemedDialog(
            dialogState = dialog,
            buttons = {
                negativeButton(
                    stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialog.hide() }
            },
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(stringResource(R.string.device_history_pint_style), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                DeviceDetailsViewModel.PointsStyle.entries.forEach { pointStyle ->
                    val isSelected = viewModel.pointsStyle == pointStyle

                    val onClick = {
                        viewModel.pointsStyle = pointStyle
                        dialog.hide()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = onClick)
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(pointStyle.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        ListItem(
            icon = painterResource(R.drawable.ic_style),
            title = stringResource(R.string.device_history_pint_style),
            subtitle = stringResource(viewModel.pointsStyle.displayNameRes),
            onClick = { dialog.show() }
        )
    }

    @Composable
    private fun HistoryPeriod(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        val dialog = rememberMaterialDialogState()
        ThemedDialog(
            dialogState = dialog,
            buttons = {
                negativeButton(
                    stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialog.hide() }
            },
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(stringResource(R.string.change_history_period_dialog), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                DeviceDetailsViewModel.HistoryPeriod.entries.forEach { period ->
                    val isSelected = viewModel.historyPeriod == period

                    val onClick = {
                        viewModel.selectHistoryPeriodSelected(period, deviceData.address, autotunePeriod = false)
                        dialog.hide()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = onClick)
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(period.displayNameRes), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        ListItem(
            icon = painterResource(R.drawable.ic_time),
            title = stringResource(R.string.device_details_history_period, stringResource(viewModel.historyPeriod.displayNameRes)),
            subtitle = stringResource(R.string.device_details_history_period_subtitle),
            onClick = { dialog.show() }
        )
    }

    @Composable
    private fun LocationHistory(
        modifier: Modifier = Modifier,
        deviceData: DeviceData, viewModel: DeviceDetailsViewModel,
        isMoving: MutableState<Boolean>,
    ) {
        RoundedBox(modifier = modifier, internalPaddings = 0.dp) {
            var mapIsReady by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Map(
                    Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    isLoading = { viewModel.markersInLoadingState = it },
                    mapIsReadyToUse = {
                        mapIsReady = true
                    },
                    isMoving = isMoving,
                )
                if (mapIsReady) {
                    MapOverlay(viewModel = viewModel)
                }
            }
            if (mapIsReady) {
                PointsStyle(viewModel)
                HistoryPeriod(deviceData = deviceData, viewModel = viewModel)
            }
        }
    }

    @Composable
    private fun MapOverlay(
        viewModel: DeviceDetailsViewModel
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            if (viewModel.pointsState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.background(color = colorResource(id = R.color.black_30), shape = RoundedCornerShape(8.dp))) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(R.string.device_details_no_location_history_for_such_period),
                            color = Color.White,
                        )
                    }
                }
            }

            if (viewModel.markersInLoadingState) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val dialog = infoDialog(
                title = stringResource(R.string.device_map_disclaimer_title),
                content = stringResource(R.string.device_map_disclaimer_content)
            )

            IconButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = {
                    dialog.show()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.device_map_disclaimer_title),
                    modifier = Modifier.size(24.dp)
                        .background(Color.Black.copy(alpha = 0.1f), shape = CircleShape),
                    tint = Color.DarkGray,
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Map(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
        isLoading: (isLoading: Boolean) -> Unit,
        mapIsReadyToUse: () -> Unit,
        isMoving: MutableState<Boolean>,
    ) {

        val scope = rememberCoroutineScope()
        val frameRate = LocalContext.current.frameRate()

        val batchProcessor = remember {
            AsyncBatchProcessor<LocationModel, MapView>(
                frameRate = frameRate,
                provideIsCancelled = { !scope.isActive },
                onBatchCompleted = { batchId, map ->
                    if (batchId % 10 == 0) {
                        map.invalidate()
                    }
                },
                processItem = { location, map ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(location.lat, location.lng)
                        title = location.time.dateTimeStringFormat("dd.MM.yy HH:mm")
                    }
                    map.overlays.add(marker)
                },
                onStart = { map ->
                    isLoading.invoke(true)
                    map.overlays.clear()
                    map.invalidate()
                },
                onComplete = { map ->
                    isLoading.invoke(false)
                    map.invalidate()
                },
                onCancelled = { map ->
                    isLoading.invoke(false)
                    map?.invalidate()
                }
            )
        }

        var mapView: MapView? by remember { mutableStateOf(null) }
        val colorScheme = MaterialTheme.colorScheme

        MapView(
            modifier = modifier.pointerInteropFilter { event ->
                if (mapView != null) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isMoving.value = true
                            false
                        }

                        MotionEvent.ACTION_UP -> {
                            isMoving.value = false
                            false
                        }

                        MotionEvent.ACTION_OUTSIDE -> {
                            isMoving.value = false
                            false
                        }

                        else -> true
                    }
                } else {
                    false
                }
            },
            onLoad = { map ->
                initMapState(map, colorScheme)
                mapIsReadyToUse.invoke()
                map.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        isMoving.value = false
                        return true
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        // do nothing
                        return true
                    }
                })
            },
            onUpdate = { map -> mapView = map }
        )
        val mapColorScheme = remember { MapColorScheme(colorScheme.scrim.copy(alpha = 0.6f), Color.Red) }

        LaunchedEffect(mapView, viewModel.pointsState, viewModel.pointsStyle) {
            if (mapView != null) {
                val mapUpdate = MapUpdate(viewModel.pointsState, viewModel.cameraState, mapView!!)
                refreshMap(mapUpdate, batchProcessor, mapColorScheme, viewModel.pointsStyle)
            }
        }
    }

    private fun initMapState(map: MapView, colorScheme: ColorScheme) {
        map.setMultiTouchControls(true)
        map.setBackgroundColor(colorScheme.surface.toArgb())
        map.minZoomLevel = MapConfig.MIN_MAP_ZOOM
        map.maxZoomLevel = MapConfig.MAX_MAP_ZOOM
        map.controller.setZoom(MapConfig.MIN_MAP_ZOOM)
    }

    private data class MapUpdate(
        val points: List<LocationModel>,
        val cameraState: DeviceDetailsViewModel.MapCameraState,
        val map: MapView,
    )

    private data class MapColorScheme(
        val lineColor: Color,
        val pointColor: Color,
    )

    private fun refreshMap(
        mapUpdate: MapUpdate,
        batchProcessor: AsyncBatchProcessor<LocationModel, MapView>,
        mapColorScheme: MapColorScheme,
        pointsStyle: DeviceDetailsViewModel.PointsStyle,
    ) {

        when (pointsStyle) {
            DeviceDetailsViewModel.PointsStyle.MARKERS -> {
                batchProcessor.process(mapUpdate.points, mapUpdate.map)
            }

            DeviceDetailsViewModel.PointsStyle.PATH -> {
                batchProcessor.cancel()
                mapUpdate.map.overlays.clear()
                val points = mapUpdate.points.map { GeoPoint(it.lat, it.lng) }
                val polyline = Polyline(mapUpdate.map).apply {
                    this.setPoints(points)
                    this.outlinePaint.apply {
                        color = mapColorScheme.lineColor.toArgb()
                    }
                }

                mapUpdate.map.overlays.add(polyline)

                val pt = SimplePointTheme(points)

                val paint = Paint().apply {
                    style = Paint.Style.FILL
                    setColor(mapColorScheme.pointColor.toArgb())
                }

                val fastPointOverlayOptions = SimpleFastPointOverlayOptions.getDefaultStyle()
                    .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                    .setPointStyle(paint)
                    .setRadius(5f)

                val fastPointOverlay = SimpleFastPointOverlay(pt, fastPointOverlayOptions)
                mapUpdate.map.overlays.add(fastPointOverlay)
                mapUpdate.map.invalidate()
            }
        }


        when (val cameraConfig = mapUpdate.cameraState) {
            is DeviceDetailsViewModel.MapCameraState.SinglePoint -> {
                Timber.d(cameraConfig.toString())
                val point = GeoPoint(cameraConfig.location.lat, cameraConfig.location.lng)
                mapUpdate.map.controller.animateTo(
                    point,
                    cameraConfig.zoom,
                    if (cameraConfig.withAnimation) MapConfig.MAP_ANIMATION else MapConfig.MAP_NO_ANIMATION
                )
                mapUpdate.map.invalidate()
            }

            is DeviceDetailsViewModel.MapCameraState.MultiplePoints -> {
                Timber.d(cameraConfig.toString())
                mapUpdate.map.post {
                    mapUpdate.map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(cameraConfig.points.map { GeoPoint(it.lat, it.lng) }),
                        cameraConfig.withAnimation,
                        mapUpdate.map.context.dpToPx(16f),
                        MapConfig.MAX_MAP_ZOOM,
                        MapConfig.MAP_ANIMATION,
                    )
                }
                mapUpdate.map.invalidate()
            }
        }
    }
}