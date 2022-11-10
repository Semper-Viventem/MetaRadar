package f.cking.software.domain

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionHelper() {

    private var context: Activity? = null

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

    private fun requestPermissions(permissions: Array<String>, permissionRequestCode: Int) {
        ActivityCompat.requestPermissions(
            requireContext(),
            permissions,
            permissionRequestCode
        )
    }

    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requireContext(): Activity = context ?: throw IllegalStateException("Activity is not attached!")

    fun setActivity(activity: Activity?) {
        context = activity
    }

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1000
        const val PERMISSIONS_BACKGROUND_REQUEST_CODE = 1001

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