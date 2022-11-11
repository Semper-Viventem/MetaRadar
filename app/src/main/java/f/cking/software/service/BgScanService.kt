package f.cking.software.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
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


class BgScanService : Service() {

    private val notificationManager: NotificationManager =
        TheApp.instance.getSystemService(NotificationManager::class.java)
    private val powerManager = TheApp.instance.getSystemService(PowerManager::class.java)

    private val handler = Handler(Looper.getMainLooper())
    private var failureScanCounter: Int = 0
    private val nextScanRunnable = Runnable {
        scan()
    }

    override fun onCreate() {
        super.onCreate()

        TheApp.instance.activeWorkId = Optional.of(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(knownDeviceCount = null))

            TheApp.instance.permissionHelper.checkBlePermissions(
                onRequestPermissions = { _, _ ->
                    stopSelf()
                },
                onPermissionGranted = ::scan
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        TheApp.instance.activeWorkId = Optional.empty()
        handler.removeCallbacks(nextScanRunnable)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun buildNotification(
        knownDeviceCount: Int?
    ): Notification {
        createChannel()

        val cancelIntent = createCloseServiceIntent(this)
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            .addAction(R.drawable.ic_cancel, "Stop", cancelPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setVibrate(null)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun scan() {
        TheApp.instance.bleScannerHelper.scan(
            scanRestricted = !powerManager.isInteractive,
            scanListener = object : BleScannerHelper.ScanListener {

                override fun onFailure() {
                    Toast.makeText(TheApp.instance, "Scan failed", Toast.LENGTH_SHORT).show()
                    failureScanCounter++

                    if (failureScanCounter >= MAX_FAILURE_SCANS_TO_CLOSE) {
                        stopSelf()
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
        private const val ACTION_STOP_SERVICE = "stop_ble_scan_service"

        private const val BG_REPEAT_INTERVAL_MS = 1000L * 60L

        private fun createCloseServiceIntent(context: Context): Intent {
            return Intent(context, BgScanService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
        }

        fun schedule(context: Context) {
            val intent = Intent(context, BgScanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            if (TheApp.instance.activeWorkId.isPresent) {
                context.startService(createCloseServiceIntent(context))
            }
        }
    }
}