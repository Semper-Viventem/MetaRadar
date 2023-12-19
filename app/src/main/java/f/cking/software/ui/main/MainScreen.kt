package f.cking.software.ui.main

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.common.BlurredNavBar
import f.cking.software.common.pxToDp
import f.cking.software.ui.GlobalUiState
import org.koin.androidx.compose.koinViewModel

object MainScreen {

    private const val NAVBAR_HEIGHT_DP = 60f

    @SuppressLint("NewApi")
    @Composable
    fun Screen() {
        val viewModel: MainViewModel = koinViewModel()
        Scaffold(
            topBar = {
                TopBar(viewModel)
            },
            content = { paddings ->
                BlurredNavBar(
                    modifier = Modifier
                        .padding(paddings)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    overlayColor = MaterialTheme.colors.primaryVariant.copy(alpha = 0.3f),
                    navBarContent = {
                        BottomNavigationBar(
                            Modifier
                                .height(NAVBAR_HEIGHT_DP.dp),
                            viewModel
                        )
                    },
                    content = {
                        viewModel.tabs.firstOrNull { it.selected }?.screen?.invoke()
                    },
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                ScanFab(viewModel)
            },
        )
        LocationDisabledDialog(viewModel)
        BluetoothDisabledDialog(viewModel)
    }

    @Composable
    private fun LocationDisabledDialog(viewModel: MainViewModel) {
        MaterialDialog(
            dialogState = viewModel.showLocationDisabledDialog,
            buttons = {
                negativeButton(stringResource(R.string.cancel))
                positiveButton(stringResource(R.string.turn_on)) {
                    viewModel.onTurnOnLocationClick()
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(id = R.string.location_is_turned_off_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(id = R.string.location_is_turned_off_subtitle))
            }
        }
    }

    @Composable
    private fun BluetoothDisabledDialog(viewModel: MainViewModel) {
        MaterialDialog(
            dialogState = viewModel.showBluetoothDisabledDialog,
            buttons = {
                negativeButton(stringResource(R.string.cancel))
                positiveButton(stringResource(R.string.turn_on)) {
                    viewModel.onTurnOnBluetoothClick()
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(id = R.string.bluetooth_is_not_available_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(id = R.string.bluetooth_is_not_available_content))
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(modifier: Modifier, viewModel: MainViewModel) {
        Box(
            modifier = modifier
                .onGloballyPositioned { GlobalUiState.setBottomOffset(navbarOffset = it.size.height.toFloat()) }
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                viewModel.tabs.forEach { tab ->
                    TabButton(viewModel = viewModel, targetTab = tab, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    @Composable
    private fun TabButton(
        viewModel: MainViewModel,
        targetTab: MainViewModel.Tab,
        modifier: Modifier = Modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .clickable { viewModel.onTabClick(targetTab) }
        ) {
            val icon = if (targetTab.selected) targetTab.selectedIconRes else targetTab.iconRes
            val font = if (targetTab.selected) FontWeight.Bold else FontWeight.SemiBold

            Image(
                painter = painterResource(id = icon),
                contentDescription = targetTab.text,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = targetTab.text, fontSize = 12.sp, fontWeight = font, color = MaterialTheme.colors.onPrimary)
        }
    }

    @Composable
    private fun ScanFab(viewModel: MainViewModel) {
        val text: String
        val icon: Int

        if (viewModel.bgServiceIsActive) {
            text = stringResource(R.string.stop)
            icon = R.drawable.ic_cancel
        } else {
            text = stringResource(R.string.scan)
            icon = R.drawable.ic_ble
        }

        val context = LocalContext.current
        val permissionsIntro = permissionsIntroDialog(
            onPassed = {
                viewModel.userHasPassedPermissionsIntro()
                viewModel.runBackgroundScanning()
            },
            onDeclined = {
                Toast.makeText(context, "The scanner cannot work without these permissions", Toast.LENGTH_SHORT).show()
            }
        )

        ExtendedFloatingActionButton(
            modifier = Modifier
                .padding(bottom = pxToDp(px = GlobalUiState.navbarOffsetPx.value).dp)
                .onGloballyPositioned { GlobalUiState.setBottomOffset(fabOffset = it.size.height.toFloat()) },
            text = { Text(text = text, fontWeight = FontWeight.Bold) },
            onClick = {
                if (viewModel.needToShowPermissionsIntro()) {
                    permissionsIntro.show()
                } else {
                    viewModel.runBackgroundScanning()
                }
            },
            icon = {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    colorFilter = ColorFilter.tint(color = MaterialTheme.colors.onSecondary)
                )
            }
        )
    }

    @Composable
    private fun permissionsIntroDialog(
        onPassed: () -> Unit,
        onDeclined: () -> Unit,
    ): MaterialDialogState {
        val state = rememberMaterialDialogState()
        MaterialDialog(
            dialogState = state,
            buttons = {
                positiveButton(stringResource(id = R.string.confirm)) {
                    state.hide()
                    onPassed.invoke()
                }
                negativeButton(stringResource(id = R.string.decline)) {
                    state.hide()
                    onDeclined.invoke()
                }
            }
        ) {
            PermissionDisclaimerContent()
        }
        return state
    }

    @Composable
    fun PermissionDisclaimerContent() {
        LazyColumn(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            item {
                Text(text = "Permissions required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                PermissionDisclaimer(
                    title = stringResource(R.string.permissions_intro_nearby_title),
                    subtitle = stringResource(R.string.permissions_intro_nearby_description),
                    icon = painterResource(R.drawable.ic_ble),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                PermissionDisclaimer(
                    title = stringResource(R.string.permissions_intro_bg_location_title),
                    subtitle = stringResource(R.string.permission_intro_bg_location_text),
                    icon = painterResource(R.drawable.ic_location),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                PermissionDisclaimer(
                    title = stringResource(R.string.permissions_intro_doze_mode_title),
                    subtitle = stringResource(R.string.permissions_intro_doze_mode_title),
                    icon = painterResource(R.drawable.ic_charge),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Text(text = stringResource(R.string.permission_data_coolect_info))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun PermissionDisclaimer(
        title: String,
        subtitle: String,
        icon: Painter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.LightGray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column() {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle)
            }
        }
    }

    @Composable
    private fun TopBar(viewModel: MainViewModel) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.app_name), color = MaterialTheme.colors.onPrimary)
            },
            actions = {
                if (viewModel.scanStarted && viewModel.bgServiceIsActive) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                } else if (viewModel.bgServiceIsActive) {
                    IconButton(onClick = { viewModel.onScanButtonClick() }) {
                        Image(
                            modifier = Modifier
                                .size(24.dp),
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
                        )
                    }
                }
            }
        )
    }
}