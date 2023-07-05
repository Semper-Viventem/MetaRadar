package f.cking.software.data.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import f.cking.software.R
import f.cking.software.domain.interactor.CheckProfileDetectionInteractor
import f.cking.software.ui.MainActivity
import kotlin.random.Random

class NotificationsHelper(
    private val context: Context,
    private val powerModeHelper: PowerModeHelper,
) {

    private val notificationManager by lazy { context.getSystemService(NotificationManager::class.java) }

    fun buildForegroundNotification(
        notificationContent: ServiceNotificationContent,
        cancelIntent: Intent,
    ): Notification {
        createServiceChannel()

        val cancelPendingIntent = PendingIntent.getService(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppPendingIntent = getOpenAppIntent()

        val body = when (notificationContent) {
            is ServiceNotificationContent.KnownDevicesAround -> context.getString(R.string.known_devices_around, notificationContent.knownDeviceCount.toString())
            is ServiceNotificationContent.TotalDevicesAround -> context.getString(R.string.total_devices_around, notificationContent.totalDeviceCount.toString())
            is ServiceNotificationContent.NoDataYet -> context.getString(R.string.ble_scanner_is_started_but_no_data)
            is ServiceNotificationContent.BluetoothIsTurnedOff -> context.getString(R.string.bluetooth_is_not_available_title)
            is ServiceNotificationContent.LocationIsTurnedOff -> context.getString(R.string.location_is_turned_off_title)
        }

        val title = if (powerModeHelper.powerMode() == PowerModeHelper.PowerMode.POWER_SAVING) {
            context.getString(R.string.app_service_title_power_saving, context.getString(R.string.app_service_title))
        } else {
            context.getString(R.string.app_service_title)
        }

        return NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_cancel, context.getString(R.string.stop), cancelPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setVibrate(null)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun notifyRadarProfile(profiles: List<CheckProfileDetectionInteractor.ProfileResult>) {
        val title = if (profiles.count() == 1) {
            val profile = profiles.first()
            context.getString(R.string.notification_profile_is_near_you, profile.profile.name)
        } else {
            context.resources.getQuantityString(R.plurals.notification_profiles_are_near_you, profiles.count(), profiles.count())
        }

        val content = profiles.flatMap { it.matched }
            .joinToString(
                separator = ", ",
                postfix = context.getString(R.string.devices_matched_postfix)
            ) { it.buildDisplayName() }

        val openAppPendingIntent = getOpenAppIntent()

        createDeviceFoundChannel()

        val notification = NotificationCompat.Builder(context, RADAR_PROFILE_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_ble)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup(RADAR_PROFILE_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    fun notifyLocationIsTurnedOff() {
        notifyError(
            title = context.getString(R.string.location_is_turned_off_title),
            content = context.getString(R.string.location_is_turned_off_subtitle),
            button = NotificationButton(
                intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                text = context.getString(R.string.turn_on),
            )
        )
    }

    fun notifyBluetoothIsTurnedOff() {
        notifyError(
            title = context.getString(R.string.bluetooth_is_not_available_title),
            content = context.getString(R.string.bluetooth_is_not_available_content),
            button = NotificationButton(
                intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                text = context.getString(R.string.turn_on)
            )
        )
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun notifyError(title: String, content: String?, button: NotificationButton?) {
        val openAppPendingIntent = getOpenAppIntent()

        createErrorsNotificationChannel()

        val notification = NotificationCompat.Builder(context, ERRORS_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_ble)
            .apply {
                if (button != null) {
                    val buttonIntent = PendingIntent.getActivity(
                        context,
                        0,
                        button.intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    addAction(R.drawable.ic_location, button.text, buttonIntent)
                }
            }
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun getOpenAppIntent(): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateNotification(serviceNotificationContent: ServiceNotificationContent, canelIntent: Intent) {
        val notification = buildForegroundNotification(serviceNotificationContent, canelIntent)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun createServiceChannel() {
        val channel = NotificationChannel(
            SERVICE_NOTIFICATION_CHANNEL,
            context.getString(R.string.scanner_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createDeviceFoundChannel() {
        val channel = NotificationChannel(
            RADAR_PROFILE_CHANNEL,
            context.getString(R.string.device_found_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { enableVibration(true) }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createErrorsNotificationChannel() {
        val channel = NotificationChannel(
            ERRORS_CHANNEL,
            context.getString(R.string.errors_notifications),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { enableVibration(true) }
        notificationManager.createNotificationChannel(channel)
    }

    data class NotificationButton(
        val intent: Intent,
        val text: String,
    )

    sealed interface ServiceNotificationContent {

        object NoDataYet : ServiceNotificationContent

        data class TotalDevicesAround(val totalDeviceCount: Int) : ServiceNotificationContent

        data class KnownDevicesAround(val knownDeviceCount: Int) : ServiceNotificationContent

        object BluetoothIsTurnedOff : ServiceNotificationContent

        object LocationIsTurnedOff : ServiceNotificationContent
    }

    companion object {
        private const val SERVICE_NOTIFICATION_CHANNEL = "service_notification_channel"
        private const val RADAR_PROFILE_CHANNEL = "radar_profile_channel"
        private const val RADAR_PROFILE_GROUP = "radar_profile_group"
        private const val ERRORS_CHANNEL = "radar_errors_channel"

        const val FOREGROUND_NOTIFICATION_ID = 42
    }
}