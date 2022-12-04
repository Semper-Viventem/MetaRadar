package f.cking.software.ui.devicedetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.domain.model.DeviceData
import f.cking.software.orNull
import org.koin.androidx.compose.koinViewModel

object DeviceDetailsScreen {

    @Composable
    fun Screen(address: String) {
        val viewModel: DeviceDetailsViewModel = koinViewModel()
        viewModel.loadDevice(address)
        val deviceData = viewModel.deviceState
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            topBar = {
                AppBar(viewModel = viewModel, deviceData.orNull())
            },
            content = {
                Box(modifier = Modifier.padding(it)) {
                    Content(viewModel, deviceData.orNull())
                }
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: DeviceDetailsViewModel, deviceData: DeviceData?) {
        TopAppBar(
            title = {
                Text(text = "Device details")
            },
            actions = {
                if (deviceData != null) {
                    IconButton(onClick = { viewModel.onFavoriteClick(deviceData) }) {
                        val iconId = if (deviceData.favorite) R.drawable.ic_star else R.drawable.ic_star_outline
                        val text = if (deviceData.favorite) "Is favorite" else "Is not favorite"
                        Icon(
                            imageVector = ImageVector.vectorResource(id = iconId),
                            contentDescription = text,
                            tint = Color.White
                        )
                    }
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
    private fun Content(viewModel: DeviceDetailsViewModel, deviceData: DeviceData?) {
        if (deviceData != null) {
            DeviceContent(viewModel = viewModel, deviceData = deviceData)
        } else {
            Progress()
        }
    }

    @Composable
    private fun Progress() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {

            CircularProgressIndicator()
        }
    }

    @Composable
    private fun DeviceContent(viewModel: DeviceDetailsViewModel, deviceData: DeviceData) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            content = {
                item { Header(viewModel = viewModel, deviceData = deviceData) }
            }
        )
    }

    @Composable
    private fun Header(viewModel: DeviceDetailsViewModel, deviceData: DeviceData) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = deviceData.buildDisplayName(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Name", fontWeight = FontWeight.Light)
            Text(text = deviceData.name ?: "N/A", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Address", fontWeight = FontWeight.Light)
            Text(text = deviceData.address, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Manufacturer", fontWeight = FontWeight.Light)
            Text(text = deviceData.manufacturerInfo?.name ?: "N/A", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(text = "Detect count: ", fontWeight = FontWeight.Light)
                Text(text = deviceData.detectCount.toString(), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "First detection", fontWeight = FontWeight.Light)
            Text(text = deviceData.firstDetectionPeriod() + " ago", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Last detection", fontWeight = FontWeight.Light)
            Text(text = deviceData.lastDetectionPeriod() + " ago", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}