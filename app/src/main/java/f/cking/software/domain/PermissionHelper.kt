package f.cking.software.domain

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionHelper() {

    private var context: Activity? = null

    fun checkBlePermissions(permissionsGranted: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allPermissionsGranted = permissions.all { checkPermission(it) }

        if (allPermissionsGranted) {
            permissionsGranted.invoke()
        } else {
            requestPermissions(permissions)
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            requireContext(),
            permissions,
            PERMISSIONS_REQUEST_CODE
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
    }
}