package f.cking.software.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import f.cking.software.R
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.helpers.BleScannerHelper
import f.cking.software.domain.helpers.PermissionHelper
import f.cking.software.domain.interactor.AnalyseScanBatchInteractor
import f.cking.software.domain.interactor.CheckProfileDetectionInteractor
import f.cking.software.domain.interactor.SaveScanBatchInteractor
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.main.MainActivity
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.lang.Math.random


class BgScanService : Service() {

    private val TAG = "BgScanService"

    private val permissionHelper: PermissionHelper by inject()
    private val bleScannerHelper: BleScannerHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val saveScanBatchInteractor: SaveScanBatchInteractor by inject()
    private val analyseScanBatchInteractor: AnalyseScanBatchInteractor by inject()
    private val checkProfileDetectionInteractor: CheckProfileDetectionInteractor by inject()

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private val handler = Handler(Looper.getMainLooper())
    private var failureScanCounter: Int = 0
    private val nextScanRunnable = Runnable {
        scan()
    }

    override fun onCreate() {
        super.onCreate()
        isActive.tryEmit(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Background service close action handled")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (intent != null && intent.action == ACTION_SCAN_NOW) {
            Log.d(TAG, "Background service scan now command")
            scan()
        } else {
            Log.d(TAG, "Background service launched")
            startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(knownDeviceCount = null))

            permissionHelper.checkBlePermissions(
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
        isActive.tryEmit(false)
        bleScannerHelper.stopScanning()
        handler.removeCallbacks(nextScanRunnable)
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun scan() {
        runBlocking {
            bleScannerHelper.scan(
                scanRestricted = isNonInteractiveMode(),
                scanDurationMs = settingsRepository.getScanDuration(),
                scanListener = object : BleScannerHelper.ScanListener {

                    override fun onFailure() {
                        failureScanCounter++

                        if (failureScanCounter >= MAX_FAILURE_SCANS_TO_CLOSE) {
                            stopSelf()
                        } else {
                            scheduleNextScan()
                        }
                    }

                    override fun onSuccess(batch: List<BleScanDevice>) {
                        handleScanResult(batch)
                    }
                }
            )
        }
    }

    private fun handleScanResult(batch: List<BleScanDevice>) {
        failureScanCounter = 0
        runBlocking {
            val analyseResult = analyseScanBatchInteractor.execute(batch)
            val profiles = checkProfileDetectionInteractor.execute(batch)
            saveScanBatchInteractor.execute(batch)
            handleProfileCheckingResult(profiles)
            handleAnalysResult(analyseResult)
            scheduleNextScan()
        }
    }

    /**
     * BLE scan is limited if device's screen is turned off
     */
    private fun isNonInteractiveMode(): Boolean {
        return !powerManager.isInteractive
    }

    private fun handleProfileCheckingResult(profiles: List<RadarProfile>) {
        if (profiles.isNotEmpty()) {
            notifyRadarProfile(profiles)
        }
    }

    private fun handleAnalysResult(result: AnalyseScanBatchInteractor.Result) {
        Log.d(
            TAG,
            "Background scan result: known_devices_count=${result.knownDevicesCount}, wanted_devices_count=${result.wanted.count()}"
        )

        updateNotification(result.knownDevicesCount)
        if (result.wanted.isNotEmpty()) {
            notifyWantedFound(result.wanted)
        }
    }

    private fun scheduleNextScan() {
        val interval = if (isNonInteractiveMode()) {
            settingsRepository.getScanRestrictedInterval()
        } else {
            settingsRepository.getScanInterval()
        }
        handler.postDelayed(nextScanRunnable, interval)
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

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
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

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
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

    private fun notifyRadarProfile(profiles: List<RadarProfile>) {
        val title = if (profiles.count() == 1) {
            val profile = profiles.first()
            "\"${profile.name}\" profile is near you!"
        } else {
            "${profiles.count()} profiles are near you!"
        }

        val content = profiles.joinToString(separator = ", ", postfix = " detected!") { it.name }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createDeviceFoundChannel()

        val notification = NotificationCompat.Builder(this, DEVICE_FOUND_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup(DEVICE_FOUND_GROUP)
            .build()

        notificationManager.notify(random().toInt(), notification)
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

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createDeviceFoundChannel()

        val notification = NotificationCompat.Builder(this, DEVICE_FOUND_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup(DEVICE_FOUND_GROUP)
            .build()

        notificationManager.notify(random().toInt(), notification)
    }

    private fun updateNotification(knownDeviceCount: Int) {
        val notification = buildForegroundNotification(knownDeviceCount)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
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
        ).apply {
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val DEVICE_FOUND_CHANNEL = "wanted_device_found"
        private const val DEVICE_FOUND_GROUP = "devices_found_group"

        private const val FOREGROUND_NOTIFICATION_ID = 42
        private const val MAX_FAILURE_SCANS_TO_CLOSE = 5

        private const val ACTION_STOP_SERVICE = "stop_ble_scan_service"
        private const val ACTION_SCAN_NOW = "ble_scan_now"

        var isActive = MutableStateFlow(false)

        private fun createCloseServiceIntent(context: Context): Intent {
            return Intent(context, BgScanService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, BgScanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            if (isActive.value) {
                context.startService(createCloseServiceIntent(context))
            }
        }

        fun scan(context: Context) {
            if (isActive.value) {
                val intent = Intent(context, BgScanService::class.java).apply {
                    action = ACTION_SCAN_NOW
                }
                context.startService(intent)
            } else {
                start(context)
            }
        }
    }
}