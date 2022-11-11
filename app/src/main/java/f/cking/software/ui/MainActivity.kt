package f.cking.software.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.TheApp
import f.cking.software.domain.helpers.PermissionHelper
import f.cking.software.domain.model.DeviceData

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val viewModel: BleScanViewModel by viewModels { BleScanViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TheApp.instance.permissionHelper.setActivity(this)

        setContent {
            Screen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_CODE && allPermissionsGranted) {
            viewModel.runBackgroundScanning()
        }
    }

    override fun onDestroy() {
        TheApp.instance.permissionHelper.setActivity(null)
        super.onDestroy()
    }

    @Composable
    @Preview(
        showBackground = true,
        showSystemUi = true,
    )
    fun Screen() {
        MaterialTheme() {
            Scaffold(
                topBar = {
                    TopBar()
                },
                content = { padding ->
                    Content(padding)
                }
            )
        }
    }

    @Composable
    fun TopBar() {
        TopAppBar(
            title = {
                Text(text = resources.getString(R.string.app_name))
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

    @Composable
    fun Content(paddingValues: PaddingValues) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            List(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { viewModel.runBackgroundScanning() },
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(text = if (TheApp.instance.backgroundScannerIsActive) "Stop background" else "Background")
                }
            }
        }
    }

    @Composable
    fun List(modifier: Modifier) {
        val data = viewModel.devicesViewState
        LazyColumn(modifier = modifier) {
            data.map { item { ListItem(listData = it) } }
        }
    }

    @Composable
    fun ListItem(listData: DeviceData) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { viewModel.onDeviceClick(listData) }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row() {
                    Text(text = listData.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (listData.favorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorite")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = listData.address)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "lifetime: ${listData.firstDetectionPeriod()} | last update: ${listData.lastDetectionPeriod()} ago",
                    fontWeight = FontWeight.Light
                )
            }
            Text(text = listData.detectCount.toString(), modifier = Modifier.padding(8.dp))
        }
    }

}
