package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import f.cking.software.data.helpers.IntentHelper.ScreenNavigation.Companion.toNavigationCommand
import f.cking.software.openUrl
import f.cking.software.ui.MainActivity
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.utils.navigation.NavigationCommand
import f.cking.software.utils.navigation.Router

class IntentHelper(
    private val activityProvider: ActivityProvider,
    private val router: Router,
) {
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
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        activityProvider.requireActivity().startActivityForResult(intent, ACTIVITY_RESULT_SELECT_FILE)
        pendingConsumers[ACTIVITY_RESULT_SELECT_FILE] = onResult
    }

    fun createFile(
        fileName: String,
        onResult: (directoryPath: Uri?) -> Unit,
    ) {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
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
        val activity = activityProvider.requireActivity()
        activity.openUrl(url)
    }

    fun openScreenIntent(screenName: ScreenNavigation): Intent {
        val intent = Intent(activityProvider.requireActivity(), MainActivity::class.java)
        intent.setAction(ACTION_OPEN_SCREEN)
        intent.putExtra(SCREEN_NAME, screenName.name)
        return intent
    }

    fun tryHandleIntent(intent: Intent): Boolean {
        if (intent.isScreenNavigation()) {
            val screenNavigation = ScreenNavigation.fromIntent(intent) ?: return false
            router.navigate(screenNavigation.toNavigationCommand())
            return true
        }
        return false
    }

    private fun Intent.isScreenNavigation(): Boolean = action == ACTION_OPEN_SCREEN

    enum class ScreenNavigation {
        MAIN,
        BACKGROUND_LOCATION_DESCRIPTION,
        ;

        companion object {
            fun fromIntent(intent: Intent): ScreenNavigation? {
                val name = intent.getStringExtra(SCREEN_NAME) ?: return null
                return entries.firstOrNull { it.name == name }
            }

            fun ScreenNavigation.toNavigationCommand(): NavigationCommand =
                when (this) {
                    MAIN -> ScreenNavigationCommands.OpenMainScreen
                    BACKGROUND_LOCATION_DESCRIPTION -> ScreenNavigationCommands.OpenBackgroundLocationScreen
                }
        }
    }

    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
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

        private const val ACTION_OPEN_SCREEN = "action_open_screen"
        private const val SCREEN_NAME = "screen_name"
    }
}
