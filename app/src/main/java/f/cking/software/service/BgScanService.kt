package f.cking.software.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import f.cking.software.R
import f.cking.software.TheApp
import f.cking.software.domain.BleDevice
import f.cking.software.domain.BleScannerHelper
import f.cking.software.domain.DeviceData
import f.cking.software.domain.DevicesRepository
import f.cking.software.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Math.random
import java.util.*


class BgScanService : Service() {

    private val TAG = "BgScanService"

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
            Log.d(TAG, "Background service close action handled")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.d(TAG, "Background service launched")
            startForeground(NOTIFICATION_ID, buildForegroundNotification(knownDeviceCount = null))

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
        Log.d(TAG, "Background service destroyed")
        TheApp.instance.activeWorkId = Optional.empty()
        handler.removeCallbacks(nextScanRunnable)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun scan() {
        TheApp.instance.bleScannerHelper.scan(
            scanDurationMs = BLE_SCAN_DURATION_MS,
            scanRestricted = !powerManager.isInteractive, // BLE scan is limited if device's screen is turned off
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
                            val result = TheApp.instance.devicesRepository.detectBatch(batch)
                            handleScanResult(result)
                            scheduleNextScan()
                        }
                    }
                }
            }
        )
    }

    private fun handleScanResult(result: DevicesRepository.Result) {
        Log.d(TAG, "Background scan result: known_devices_count=${result.knownDevicesCount}, wanted_devices_count=${result.wanted.count()}")

        updateNotification(result.knownDevicesCount)
        if (result.wanted.isNotEmpty()) {
            notifyWantedFound(result.wanted)
        }
    }

    private fun scheduleNextScan() {
        handler.postDelayed(nextScanRunnable, BG_REPEAT_INTERVAL_MS)
    }

    private fun buildForegroundNotification(
        knownDeviceCount: Int?
    ): Notification {
        createServiceChannel()

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

    private fun notifyWantedFound(wantedDevices: Set<DeviceData>) {
        val title = if (wantedDevices.count() == 1) {
            val device = wantedDevices.first()
            "\"${device.buildDisplayName()}\" device is near you!"
        } else {
            "${wantedDevices.count()} wanted devices are near you!"
        }

        val content = wantedDevices.joinToString(",\n", postfix = ".") { device ->
            "${device.buildDisplayName()}, last seen ${device.lastDetectionPeriod()} ago"
        }

        val openAppIntent = Intent(TheApp.instance, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            TheApp.instance,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createDeviceFoundChannel()

        val notification = NotificationCompat.Builder(TheApp.instance, DEVICE_FOUND_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        notificationManager.notify(random().toInt(), notification)
    }

    private fun updateNotification(knownDeviceCount: Int) {
        val notification = buildForegroundNotification(knownDeviceCount)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createServiceChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "Scan BLE in background",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createDeviceFoundChannel() {
        val channel = NotificationChannel(
            DEVICE_FOUND_CHANNEL,
            "Wanted device found",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val DEVICE_FOUND_CHANNEL = "wanted_device_found"
        private const val NOTIFICATION_ID = 42
        private const val MAX_FAILURE_SCANS_TO_CLOSE = 5
        private const val ACTION_STOP_SERVICE = "stop_ble_scan_service"

        private const val BLE_SCAN_DURATION_MS = 5_000L // 5 sec
        private const val BG_REPEAT_INTERVAL_MS = 30_000L // 30 sec

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