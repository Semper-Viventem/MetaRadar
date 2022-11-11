package f.cking.software.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import f.cking.software.R
import f.cking.software.TheApp
import f.cking.software.domain.BleDevice
import f.cking.software.domain.BleScannerHelper
import f.cking.software.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


class BgScanWorker(appContext: Context, workerParams: WorkerParameters) : ListenableWorker(appContext, workerParams) {

    private val notificationManager: NotificationManager =
        TheApp.instance.getSystemService(NotificationManager::class.java)

    lateinit var completer: CallbackToFutureAdapter.Completer<Result>
    private val handler = Handler(Looper.getMainLooper())
    private var failureScanCounter: Int = 0
    private val nextScanRunnable = Runnable {
        scan()
    }

    override fun startWork(): ListenableFuture<Result> {
        TheApp.instance.activeWorkId = Optional.of(id)
        return CallbackToFutureAdapter.getFuture { completer ->
            this.completer = completer
            setForegroundAsync(buildForegroundInfo())

            TheApp.instance.permissionHelper.checkBlePermissions(
                onRequestPermissions = { _, _ ->
                    complete(Result.failure())
                },
                onPermissionGranted = ::scan
            )
        }
    }

    override fun onStopped() {
        handler.removeCallbacks(nextScanRunnable)
        notificationManager.cancel(NOTIFICATION_ID)
        complete(Result.failure())
        super.onStopped()
    }

    private fun buildForegroundInfo(): ForegroundInfo {
        createChannel()

        val notification = buildNotification(knownDeviceCount = null)

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        knownDeviceCount: Int?
    ): Notification {

        val cancelIntent = WorkManager.getInstance(TheApp.instance)
            .createCancelPendingIntent(id)

        val openAppIntent = Intent(TheApp.instance, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            TheApp.instance,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (knownDeviceCount == null) {
            "BLE scanner is started but there is no data yet"
        } else if (knownDeviceCount > 0) {
            "$knownDeviceCount known devices around."
        } else {
            "There are no known devices around"
        }

        return NotificationCompat.Builder(TheApp.instance, NOTIFICATION_CHANNEL)
            .setContentTitle("MetaRadar service")
            .setContentText(body)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_cancel, "Stop", cancelIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setVibrate(null)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun complete(result: Result) {
        TheApp.instance.activeWorkId = Optional.empty()
        completer.set(result)
    }

    private fun scan() {
        TheApp.instance.bleScannerHelper.scan(
            scanListener = object : BleScannerHelper.ScanListener {

                override fun onFailure() {
                    Toast.makeText(TheApp.instance, "Scan failed", Toast.LENGTH_SHORT).show()
                    failureScanCounter++

                    if (failureScanCounter >= MAX_FAILURE_SCANS_TO_CLOSE) {
                        complete(Result.failure())
                    } else {
                        scheduleNextScan()
                    }
                }

                override fun onSuccess(batch: List<BleDevice>) {
                    failureScanCounter = 0
                    runBlocking {
                        launch(Dispatchers.IO) {
                            val knownDeviceCount = TheApp.instance.devicesRepository.detectBatch(batch)
                            updateNotification(knownDeviceCount)
                            scheduleNextScan()
                        }
                    }
                }
            }
        )
    }

    private fun updateNotification(knownDeviceCount: Int) {
        val notification = buildNotification(knownDeviceCount)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleNextScan() {
        handler.postDelayed(nextScanRunnable, BG_REPEAT_INTERVAL_MS)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "Scan BLE in background",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val NOTIFICATION_ID = 42
        private const val MAX_FAILURE_SCANS_TO_CLOSE = 5

        private const val BG_REPEAT_INTERVAL_MS = 1000L * 60L

        fun schedule(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequest.Builder(BgScanWorker::class.java)
                        .build()
                )
        }

        fun stop(context: Context) {
            if (TheApp.instance.activeWorkId.isPresent) {
                WorkManager.getInstance(context)
                    .cancelWorkById(TheApp.instance.activeWorkId.get())
            }
        }
    }
}