package f.cking.software

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private lateinit var bluetoothScanner: BluetoothLeScanner
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var scanStarted by mutableStateOf(false)

    private val devices = mutableSetOf<String>()
    private var devicesViewState by mutableStateOf<Set<String>>(devices)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Content()
        }

        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        checkPermissionAndRunScanning()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            checkPermissionAndRunScanning()
        }
    }

    private fun checkPermissionAndRunScanning() {
        val permissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allPermissionsGranted = permissions.all { checkPermission(it) }

        if (allPermissionsGranted) {
            scanBle()
        } else {
            requestPermissions(permissions)
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            PERMISSIONS_REQUEST_CODE
        )
    }

    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            devices.add("${result.device.name}\n${result.device.address}")
            devicesViewState = devices
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "Scan failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanBle() {
        if (!scanStarted) {
            handler.postDelayed(::cancelScanning, 10_000L)
            scanStarted = true
            bluetoothScanner.startScan(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning() {
        scanStarted = false
        bluetoothScanner.stopScan(callback)
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
                        onClick = { checkPermissionAndRunScanning() },
                        enabled = !scanStarted
                    ) {
                        Text(text = "scan", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    if (scanStarted) {
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
        val data = devicesViewState
        LazyColumn(modifier = modifier) {
            data.map { item { ListItem(listData = it) } }
        }
    }

    @Composable
    fun ListItem(listData: String) {
        Text(text = listData, modifier = Modifier.padding(16.dp))
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1000
    }
}