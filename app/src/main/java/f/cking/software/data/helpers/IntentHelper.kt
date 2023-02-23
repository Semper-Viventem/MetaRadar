package f.cking.software.data.helpers

import android.app.Activity
import android.content.Intent
import android.net.Uri

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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
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

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val consumer = pendingConsumers[requestCode]
        if (resultCode == Activity.RESULT_OK) {
            consumer?.invoke(data?.data)
        } else {
            consumer?.invoke(null)
        }
    }

    fun restartTheApp() {
        val ctx = activityProvider.requireActivity()
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ctx.startActivity(intent)
        System.exit(0)
    }

    companion object {
        private const val ACTIVITY_RESULT_SELECT_DIRECTORY = 1
        private const val ACTIVITY_RESULT_SELECT_FILE = 2
        private const val ACTIVITY_RESULT_CREATE_FILE = 3
    }
}