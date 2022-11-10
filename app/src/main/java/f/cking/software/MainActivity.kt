package f.cking.software

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val viewModel: BleScanViewModel by viewModels { BleScanViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TheApp.instance.permissionHelper.setActivity(this)
        setContent {
            Content()
        }

        viewModel.onActivityAttached()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_CODE) {
            viewModel.onPermissionResult()
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
    fun Content() {
        MaterialTheme() {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
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
                        onClick = { viewModel.onScanButtonClick() },
                        enabled = !viewModel.scanStarted
                    ) {
                        Text(text = "scan", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    if (viewModel.scanStarted) {
                        CircularProgressIndicator(
                            Modifier
                                .width(24.dp)
                                .height(24.dp)
                        )
                    }
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
    fun ListItem(listData: DevicesRepository.DeviceData) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listData.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = listData.address)
                val lifetimeMs = System.currentTimeMillis() - listData.firstDetectTimeMs
                val lifetimeMin = lifetimeMs / 1000 / 60
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "lifetime: $lifetimeMin min", fontWeight = FontWeight.Light)
            }
            Text(text = listData.detectCount.toString(), modifier = Modifier.padding(8.dp))
        }
    }

}
