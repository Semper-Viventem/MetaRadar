package f.cking.software.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import f.cking.software.R
import f.cking.software.TheApp
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
        complete(Result.failure())
        super.onStopped()
    }

    private fun buildForegroundInfo(): ForegroundInfo {
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

    private fun complete(result: Result) {
        TheApp.instance.activeWorkId = Optional.empty()
        completer.set(result)
    }

    private fun scan() {
        TheApp.instance.bleScannerHelper.scan { batch ->
            runBlocking {
                launch(Dispatchers.IO) {
                    TheApp.instance.devicesRepository.detectBatch(batch)
                    scheduleNextScan()
                }
            }
        }
    }

    private fun scheduleNextScan() {
        handler.postDelayed(nextScanRunnable, BG_REPEAT_INTERVAL_MS)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "Scan BLE in background",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val NOTIFICATION_ID = 42

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