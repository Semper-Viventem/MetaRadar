package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import f.cking.software.R

class IntentHelper(private val activityProvider: ActivityProvider) {

    /**
     * TODO: this code us unsafe
     */
    private val pendingConsumers = mutableMapOf<Int, (result: Uri?) -> Unit>()

    fun selectDirectory(onResult: (directoryPath: Uri?) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activityProvider.requireActivity().startActivityForResult(intent, ACTIVITY_RESULT_SELECT_DIRECTORY)
        pendingConsumers[ACTIVITY_RESULT_SELECT_DIRECTORY] = onResult
    }

    fun selectFile(onResult: (filePath: Uri?) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        activityProvider.requireActivity().startActivityForResult(intent, ACTIVITY_RESULT_SELECT_FILE)
        pendingConsumers[ACTIVITY_RESULT_SELECT_FILE] = onResult
    }

    fun createFile(fileName: String, onResult: (directoryPath: Uri?) -> Unit) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
            type = "application/sqlite"
        }
        activityProvider.requireActivity().startActivityForResult(intent, ACTIVITY_RESULT_CREATE_FILE)
        pendingConsumers[ACTIVITY_RESULT_CREATE_FILE] = onResult
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activityProvider.requireActivity().getPackageName(), null)
        intent.data = uri
        activityProvider.requireActivity().startActivity(intent)
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activityProvider.requireActivity().startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun openBluetoothSettings() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activityProvider.requireActivity().startActivity(intent)
    }

    fun openUrl(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        val activity = activityProvider.requireActivity()
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, activity.getString(R.string.cannot_open_the_url), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val consumer = pendingConsumers[requestCode]
        if (resultCode == Activity.RESULT_OK) {
            consumer?.invoke(data?.data)
        } else {
            consumer?.invoke(null)
        }
    }

    companion object {
        private const val ACTIVITY_RESULT_SELECT_DIRECTORY = 1
        private const val ACTIVITY_RESULT_SELECT_FILE = 2
        private const val ACTIVITY_RESULT_CREATE_FILE = 3
    }
}