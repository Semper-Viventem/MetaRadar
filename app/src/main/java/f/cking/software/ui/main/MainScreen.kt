package f.cking.software.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import org.koin.androidx.compose.koinViewModel

object MainScreen {

    @Composable
    fun Screen() {
        val viewModel: MainViewModel = koinViewModel()
        Scaffold(
            topBar = {
                TopBar(viewModel)
            },
            content = { paddings ->
                Box(modifier = Modifier.padding(paddings)) {
                    viewModel.tabs.firstOrNull { it.selected }?.screen?.invoke()
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                ScanFab(viewModel)
            },
            bottomBar = {
                BottomNavigationBar(viewModel)
            }
        )
    }

    @Composable
    private fun BottomNavigationBar(viewModel: MainViewModel) {
        BottomAppBar {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
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
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = targetTab.text, fontSize = 12.sp, fontWeight = font, color = Color.White)
        }
    }

    @Composable
    private fun ScanFab(viewModel: MainViewModel) {
        val text: String
        val icon: Int

        if (viewModel.bgServiceIsActive) {
            text = "Stop"
            icon = R.drawable.ic_cancel
        } else {
            text = "Scan"
            icon = R.drawable.ic_ble
        }

        ExtendedFloatingActionButton(
            text = { Text(text = text, fontWeight = FontWeight.Bold) },
            onClick = { viewModel.runBackgroundScanning() },
            icon = {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    colorFilter = ColorFilter.tint(color = Color.White)
                )
            }
        )
    }

    @Composable
    private fun TopBar(viewModel: MainViewModel) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.app_name))
            },
            actions = {
                if (viewModel.scanStarted) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                } else {
                    IconButton(onClick = { viewModel.onScanButtonClick() }) {
                        Image(
                            modifier = Modifier
                                .size(24.dp),
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "refresh",
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
        )
    }
}