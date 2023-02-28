package f.cking.software.data.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import f.cking.software.R

class PermissionHelper(
    private val context: Context,
    private val activityProvider: ActivityProvider,
    private val intentHelper: IntentHelper,
) {

    private var pending: (() -> Unit)? = null
    private var permissionRequestTime: Long? = null

    fun checkBlePermissions(
        onRequestPermissions: (permissions: Array<String>, permissionRequestCode: Int, pendingFun: () -> Unit) -> Unit = ::requestPermissions,
        permissions: Array<String> = BLE_PERMISSIONS,
        permissionRequestCode: Int = PERMISSIONS_REQUEST_CODE,
        onPermissionGranted: () -> Unit,
    ) {

        val allPermissionsGranted = permissions.all { checkPermission(it) }

        if (allPermissionsGranted) {
            onPermissionGranted.invoke()
        } else {
            onRequestPermissions.invoke(permissions, permissionRequestCode, onPermissionGranted)
        }
    }

    @SuppressLint("BatteryLife")
    fun checkDozeModePermission() {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val allowedInDozeMode = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        if (!allowedInDozeMode) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            activityProvider.requireActivity().startActivity(intent)
        }
    }

    fun onPermissionGranted(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        val requestTime = permissionRequestTime
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (allPermissionsGranted) {
                pending?.invoke()
            } else if (requestTime != null && System.currentTimeMillis() - requestTime <= MIN_REQUEST_TIME_MS) {
                Toast.makeText(activityProvider.requireActivity(), context.getString(R.string.grant_permissions_manually), Toast.LENGTH_LONG).show()
                intentHelper.openAppSettings()
            }
        }
    }

    private fun requestPermissions(
        permissions: Array<String>,
        permissionRequestCode: Int,
        onPermissionGranted: () -> Unit
    ) {
        this.pending = onPermissionGranted
        permissionRequestTime = System.currentTimeMillis()
        ActivityCompat.requestPermissions(
            activityProvider.requireActivity(),
            permissions,
            permissionRequestCode
        )
    }

    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1000
        const val MIN_REQUEST_TIME_MS = 100L

        val BACKGROUND_LOCATION = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val BLE_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    }
}