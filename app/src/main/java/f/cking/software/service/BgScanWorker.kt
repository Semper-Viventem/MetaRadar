package f.cking.software.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import f.cking.software.R
import f.cking.software.TheApp
import f.cking.software.domain.BleDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


class BgScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val notificationManager: NotificationManager =
        TheApp.instance.getSystemService(NotificationManager::class.java)

    private var result: Result? = null

    override fun doWork(): Result {

        setForegroundAsync(buildForegroundInfo())

        TheApp.instance.permissionHelper.checkBlePermissions(
            onRequestPermissions = { _, _ ->
                result = Result.failure()
            },
            onPermissionGranted = ::scan
        )
        while (result == null) {
            // waiting
        }

        return result!!
    }

    fun buildForegroundInfo(): ForegroundInfo {
        createChannel()

        val intent = WorkManager.getInstance(TheApp.instance)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(TheApp.instance, NOTIFICATION_CHANNEL)
            .setContentTitle("BLE scan...")
            .setContentText("Scan ble environment in background")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_ble)
            .addAction(R.drawable.ic_cancel, "cancel", intent)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun scan() {
        TheApp.instance.bleScannerHelper.scan { batch ->
            handleScanned(batch)
        }
    }

    private fun handleScanned(batch: List<BleDevice>) {
        runBlocking {
            launch(Dispatchers.IO) {
                TheApp.instance.devicesRepository.detectBatch(batch)
                result = Result.success()
            }
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "Scan ble in background",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val NOTIFICATION_ID = 42

        private const val BG_REPEAT_INTERVAL_MIN = 15L

        fun schedule(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(
                    PeriodicWorkRequest.Builder(
                        BgScanWorker::class.java,
                        BG_REPEAT_INTERVAL_MIN,
                        TimeUnit.MINUTES
                    ).build()
                )
        }
    }
}