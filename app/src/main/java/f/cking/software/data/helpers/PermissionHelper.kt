package f.cking.software.data.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat

class PermissionHelper(
    private val context: Context,
) {

    private var activity: Activity? = null

    fun checkBlePermissions(
        onRequestPermissions: (permissions: Array<String>, permissionRequestCode: Int) -> Unit = ::requestPermissions,
        permissions: Array<String> = BLE_PERMISSIONS,
        permissionRequestCode: Int = PERMISSIONS_REQUEST_CODE,
        onPermissionGranted: () -> Unit,
    ) {

        val allPermissionsGranted = permissions.all { checkPermission(it) }

        if (allPermissionsGranted) {
            onPermissionGranted.invoke()
        } else {
            onRequestPermissions.invoke(permissions, permissionRequestCode)
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
            context.startActivity(intent)
        }
    }

    private fun requestPermissions(permissions: Array<String>, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(
            requireActivity(),
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

    private fun requireActivity(): Activity = activity ?: throw IllegalStateException("Activity is not attached!")

    fun setActivity(activity: Activity?) {
        this.activity = activity
    }

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1000

        val BACKGROUND_LOCATION = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val BLE_PERMISSIONS: Array<String> = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
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