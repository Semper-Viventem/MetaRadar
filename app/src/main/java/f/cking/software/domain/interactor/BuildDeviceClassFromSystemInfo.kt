package f.cking.software.domain.interactor

import android.bluetooth.BluetoothClass
import f.cking.software.domain.model.DeviceClass
import f.cking.software.domain.model.DeviceData
import f.cking.software.extract16BitUuid

object BuildDeviceClassFromSystemInfo {
    private const val MAJOR_BIT_MASK = 0x1F00

    /**
     * Build a [DeviceClass] from the [android.bluetooth.BluetoothClass.Device].
     */
    fun execute(deviceData: DeviceData): DeviceClass {
        val systemClass = deviceData.deviceClass

        if (systemClass == null) return DeviceClass.Unknown

        val major = systemClass and MAJOR_BIT_MASK
        var result =
            when (major) {
                BluetoothClass.Device.Major.PHONE -> {
                    when (systemClass) {
                        BluetoothClass.Device.PHONE_CELLULAR -> DeviceClass.Phone.Cellular
                        BluetoothClass.Device.PHONE_CORDLESS -> DeviceClass.Phone.Cordless
                        BluetoothClass.Device.PHONE_ISDN -> DeviceClass.Phone.Isdn
                        BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY -> DeviceClass.Phone.ModemOrGateway
                        BluetoothClass.Device.PHONE_SMART -> DeviceClass.Phone.Smartphone
                        else -> DeviceClass.Phone.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.COMPUTER -> {
                    when (systemClass) {
                        BluetoothClass.Device.COMPUTER_DESKTOP -> DeviceClass.Computer.Desktop
                        BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> DeviceClass.Computer.Laptop
                        BluetoothClass.Device.COMPUTER_LAPTOP -> DeviceClass.Computer.Laptop
                        BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> DeviceClass.Computer.Laptop
                        BluetoothClass.Device.COMPUTER_SERVER -> DeviceClass.Computer.Server
                        BluetoothClass.Device.COMPUTER_WEARABLE -> DeviceClass.Computer.Wearable
                        else -> DeviceClass.Computer.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                    when (systemClass) {
                        BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER -> DeviceClass.AudioVideo.Camcorder
                        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceClass.AudioVideo.CarAudio
                        BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> DeviceClass.AudioVideo.HandsFree
                        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceClass.AudioVideo.Headphones
                        BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> DeviceClass.AudioVideo.HifiAudio
                        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceClass.AudioVideo.Loudspeaker
                        BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE -> DeviceClass.AudioVideo.Microphone
                        BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> DeviceClass.AudioVideo.PortableAudio
                        BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> DeviceClass.AudioVideo.SetTopBox
                        BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> DeviceClass.AudioVideo.Uncategorised
                        BluetoothClass.Device.AUDIO_VIDEO_VCR -> DeviceClass.AudioVideo.Vcr
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> DeviceClass.AudioVideo.VideoCamera
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING -> DeviceClass.AudioVideo.VideoConferencing
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> DeviceClass.AudioVideo.VideoDisplayAndLoudspeaker
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> DeviceClass.AudioVideo.VideoGamingToy
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR -> DeviceClass.AudioVideo.VideoMonitor
                        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceClass.AudioVideo.WearableHeadset
                        else -> DeviceClass.AudioVideo.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.WEARABLE -> {
                    when (systemClass) {
                        BluetoothClass.Device.WEARABLE_GLASSES -> DeviceClass.Wearable.Glasses
                        BluetoothClass.Device.WEARABLE_HELMET -> DeviceClass.Wearable.Helmet
                        BluetoothClass.Device.WEARABLE_JACKET -> DeviceClass.Wearable.Jacket
                        BluetoothClass.Device.WEARABLE_PAGER -> DeviceClass.Wearable.Pager
                        BluetoothClass.Device.WEARABLE_WRIST_WATCH -> DeviceClass.Wearable.WristWatch
                        else -> DeviceClass.Wearable.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.TOY -> {
                    when (systemClass) {
                        BluetoothClass.Device.TOY_CONTROLLER -> DeviceClass.Toy.Controller
                        BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE -> DeviceClass.Toy.Doll
                        BluetoothClass.Device.TOY_GAME -> DeviceClass.Toy.Game
                        BluetoothClass.Device.TOY_ROBOT -> DeviceClass.Toy.Robot
                        BluetoothClass.Device.TOY_VEHICLE -> DeviceClass.Toy.Vehicle
                        else -> DeviceClass.Toy.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.HEALTH -> {
                    when (systemClass) {
                        BluetoothClass.Device.HEALTH_BLOOD_PRESSURE -> DeviceClass.Health.BloodPressure
                        BluetoothClass.Device.HEALTH_THERMOMETER -> DeviceClass.Health.Thermometer
                        BluetoothClass.Device.HEALTH_WEIGHING -> DeviceClass.Health.Weighing
                        else -> DeviceClass.Health.Uncategorised
                    }
                }

                BluetoothClass.Device.Major.PERIPHERAL -> {
                    when (systemClass) {
                        BluetoothClass.Device.PERIPHERAL_KEYBOARD -> DeviceClass.Peripheral.Keyboard
                        BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING -> DeviceClass.Peripheral.KeyboardPointing
                        BluetoothClass.Device.PERIPHERAL_POINTING -> DeviceClass.Peripheral.Pointing
                        else -> DeviceClass.Peripheral.Uncategorised
                    }
                }

                else -> DeviceClass.Unknown
            }

        if (result is DeviceClass.Unknown) {
            result = getCategoryByServiceUuid(deviceData)
        }

        if (result is DeviceClass.Unknown) {
            result = getCategoryByDeviceName(deviceData)
        }

        return result
    }

    private fun getCategoryByDeviceName(deviceData: DeviceData): DeviceClass =
        NAME_SUBSTRING_TO_TYPE.entries
            .firstOrNull { (substring, _) ->
                deviceData.resolvedName.orEmpty().contains(substring, ignoreCase = true)
            }?.value ?: DeviceClass.Unknown

    private fun getCategoryByServiceUuid(deviceData: DeviceData): DeviceClass {
        val deviceServicesShorten = deviceData.servicesUuids.map { extract16BitUuid(it).orEmpty().lowercase() }

        val matched: List<DeviceClass> =
            deviceServicesShorten.mapNotNull { shortenService ->
                SERVICE_UUID_TO_TYPE[shortenService]
            }

        return matched.firstOrNull() ?: DeviceClass.Unknown
    }

    private val SERVICE_UUID_TO_TYPE =
        mapOf(
            // Tracking Tags
            "fe2c" to DeviceClass.Beacon.AirTag, // apple find my network
            "feed" to DeviceClass.Beacon.Uncategorised, // tile tracker
            "fd50" to DeviceClass.Beacon.IBeacon, // samsung smart tag
            "181a" to DeviceClass.Beacon.Uncategorised,
            "fe9a" to DeviceClass.Beacon.Uncategorised,
            "1843" to DeviceClass.AudioVideo.Uncategorised, // audio input control service
            "1858" to DeviceClass.AudioVideo.Uncategorised, // gaming audio service
            "180f" to DeviceClass.AudioVideo.Uncategorised, // "Battery Service (common in wireless earbuds and headphones)",
            "1844" to DeviceClass.AudioVideo.Uncategorised, // "Battery Service (common in wireless earbuds and headphones)",
            "180d" to DeviceClass.Health.HeartPulseRate, // "Heart Rate Monitor (wearable, fitness tracker)",
            "180c" to DeviceClass.Health.Uncategorised, // "Cycling Speed and Cadence Sensor",
            "1816" to DeviceClass.Health.Uncategorised, // "Cycling Power Meter",
            "181f" to DeviceClass.Health.Glucose, // "Continuous Glucose Monitor",
            "1810" to DeviceClass.Health.BloodPressure, // "Blood Pressure Monitor",
            "1809" to DeviceClass.Health.Thermometer, // "Thermometer",
            "181c" to DeviceClass.Health.PulseOximeter, // "Blood Oxygen Sensor (Pulse Oximeter)",
            "1822" to DeviceClass.Health.PulseOximeter, // "Pulse Oximeter",
            "1812" to DeviceClass.Peripheral.Keyboard, // "Human Interface Device (HID - Keyboard, Mouse, Gamepad)",
            "1824" to DeviceClass.Peripheral.Uncategorised, // "Transport Discovery (Smart Remote, Controller)",
            "183E" to DeviceClass.Health.Uncategorised, // "Physical Activity Monitor",
            "1840" to DeviceClass.Health.Uncategorised, // "Generic Health Sensor",
            "1851" to DeviceClass.AudioVideo.Uncategorised, // "Media Control Service",
            "1853" to DeviceClass.AudioVideo.Uncategorised, // Common Audio Service,
        )

    private val NAME_SUBSTRING_TO_TYPE =
        mapOf(
            "buds" to DeviceClass.AudioVideo.Headphones,
            "phone" to DeviceClass.Phone.Smartphone,
            "pixel 8" to DeviceClass.Phone.Smartphone,
            "pixel 9" to DeviceClass.Phone.Smartphone,
            "pixel 7" to DeviceClass.Phone.Smartphone,
            "watch" to DeviceClass.Wearable.WristWatch,
            "STANMORE" to DeviceClass.AudioVideo.Loudspeaker,
            "MONITOR II" to DeviceClass.AudioVideo.Headphones,
            "SOUNDLINK" to DeviceClass.AudioVideo.PortableAudio,
            "Xbox" to DeviceClass.AudioVideo.VideoGamingToy,
            "airtag" to DeviceClass.Beacon.AirTag,
            "ibeacon" to DeviceClass.Beacon.IBeacon,
            "smart tag" to DeviceClass.Beacon.Uncategorised,
            "\" Odyssey" to DeviceClass.AudioVideo.VideoMonitor,
            "meta quest" to DeviceClass.Wearable.Glasses,
            " TV" to DeviceClass.AudioVideo.VideoDisplayAndLoudspeaker,
            "TV " to DeviceClass.AudioVideo.VideoDisplayAndLoudspeaker,
            "MacBook" to DeviceClass.Computer.Laptop,
            "iMac" to DeviceClass.Computer.Desktop,
            "Macmini" to DeviceClass.Computer.Desktop,
            "Mac1" to DeviceClass.Computer.Desktop,
            "Mac2" to DeviceClass.Computer.Desktop,
            "iPad" to DeviceClass.Phone.Smartphone,
        )
}
