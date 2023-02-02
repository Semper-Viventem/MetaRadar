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
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.AnalyseScanBatchInteractor
import f.cking.software.domain.interactor.CheckProfileDetectionInteractor
import f.cking.software.domain.interactor.SaveReportInteractor
import f.cking.software.domain.interactor.SaveScanBatchInteractor
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.JournalEntry
import f.cking.software.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject
import kotlin.random.Random


class BgScanService : Service() {

    private val TAG = "BgScanService"

    private val permissionHelper: PermissionHelper by inject()
    private val bleScannerHelper: BleScannerHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val locationProvider: LocationProvider by inject()

    private val saveScanBatchInteractor: SaveScanBatchInteractor by inject()
    private val analyseScanBatchInteractor: AnalyseScanBatchInteractor by inject()
    private val saveReportInteractor: SaveReportInteractor by inject()

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private val handler = Handler(Looper.getMainLooper())
    private var failureScanCounter: Int = 0
    private val nextScanRunnable = Runnable {
        scan()
    }

    private val bleListener = object : BleScannerHelper.ScanListener {
        override fun onFailure(exception: Exception) {
            handleError(exception)
        }

        override fun onSuccess(batch: List<BleScanDevice>) {
            handleScanResult(batch)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        isActive.tryEmit(true)
    }

    private fun handleError(exception: Throwable) {
        failureScanCounter++

        reportError(exception)
        if (failureScanCounter >= MAX_FAILURE_SCANS_TO_CLOSE) {
            reportError(RuntimeException("Stop service after $MAX_FAILURE_SCANS_TO_CLOSE exceptions"))
            stopSelf()
        } else {
            scheduleNextScan()
        }
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
            locationProvider.startLocationLeastening()

            permissionHelper.checkBlePermissions(
                onRequestPermissions = { _, _, _ ->
                    reportError(IllegalStateException("BLE Service is started but permissins are not granted"))
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
        scope.cancel()
        isActive.tryEmit(false)
        bleScannerHelper.stopScanning()
        locationProvider.stopLocationListening()
        handler.removeCallbacks(nextScanRunnable)
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun scan() {
        scope.launch {
            bleScannerHelper.scan(
                scanRestricted = isNonInteractiveMode(),
                scanDurationMs = settingsRepository.getScanDuration(),
                scanListener = bleListener,
            )
        }
    }

    private fun handleScanResult(batch: List<BleScanDevice>) {
        scope.launch {
            try {
                val analyseResult = analyseScanBatchInteractor.execute(batch)
                withContext(Dispatchers.Default) {
                    saveScanBatchInteractor.execute(batch)
                }
                withContext(Dispatchers.Main) {
                    handleAnalysResult(analyseResult)
                }
                scheduleNextScan()
                failureScanCounter = 0
            } catch (exception: Throwable) {
                handleError(exception)
            }
        }
    }

    /**
     * BLE scan is limited if device's screen is turned off
     */
    private fun isNonInteractiveMode(): Boolean {
        return !powerManager.isInteractive
    }

    private fun handleProfileCheckingResult(profiles: List<CheckProfileDetectionInteractor.ProfileResult>) {
        if (profiles.isNotEmpty()) {
            notifyRadarProfile(profiles)
        }
    }

    private fun handleAnalysResult(result: AnalyseScanBatchInteractor.Result) {
        Log.d(
            TAG,
            "Background scan result: known_devices_count=${result.knownDevicesCount}, matched_profiles=${result.matchedProfiles.count()}"
        )

        updateNotification(result.knownDevicesCount)
        handleProfileCheckingResult(result.matchedProfiles)
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

    private fun notifyRadarProfile(profiles: List<CheckProfileDetectionInteractor.ProfileResult>) {
        val title = if (profiles.count() == 1) {
            val profile = profiles.first()
            "\"${profile.profile.name}\" profile is near you!"
        } else {
            "${profiles.count()} profiles are near you!"
        }

        val content = profiles.flatMap { it.matched }
            .joinToString(separator = ", ", postfix = " devices matched!.") { it.buildDisplayName() }

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
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
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

    private fun reportError(error: Throwable) {
        Log.e(TAG, error.message.orEmpty(), error)
        scope.launch {
            val report = JournalEntry.Report.Error(
                title = "[BLE Service Error]: ${error.message ?: error::class.java}",
                stackTrace = error.stackTraceToString(),
            )
            saveReportInteractor.execute(report)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL = "background_scan"
        private const val DEVICE_FOUND_CHANNEL = "wanted_device_found"
        private const val DEVICE_FOUND_GROUP = "devices_found_group"

        private const val FOREGROUND_NOTIFICATION_ID = 42
        private const val MAX_FAILURE_SCANS_TO_CLOSE = 10

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