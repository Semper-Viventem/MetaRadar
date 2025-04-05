package f.cking.software.utils.graphic

import f.cking.software.R
import f.cking.software.domain.model.DeviceClass
import f.cking.software.domain.model.DeviceData

object GetIconForDeviceClass {
    /**
     * @return icon resource based on [DeviceClass]
     */
    fun getIcon(device: DeviceData): Int {
        val manufacturer = device.manufacturerInfo

        return when (device.resolvedDeviceClass) {
            is DeviceClass.Phone -> {
                if (manufacturer?.isApple() == true) {
                    R.drawable.ic_phone_iphone
                } else {
                    R.drawable.ic_phone_android
                }
            }

            is DeviceClass.Computer.Laptop -> R.drawable.ic_laptop
            is DeviceClass.Computer.Desktop -> R.drawable.ic_desctop
            is DeviceClass.Computer -> R.drawable.ic_laptop

            is DeviceClass.AudioVideo.VideoMonitor -> R.drawable.ic_monitor
            is DeviceClass.AudioVideo.VideoDisplayAndLoudspeaker -> R.drawable.ic_monitor
            is DeviceClass.AudioVideo.Microphone -> R.drawable.ic_mic
            is DeviceClass.AudioVideo.Headphones -> R.drawable.ic_headphones
            is DeviceClass.AudioVideo.HandsFree -> R.drawable.ic_headset_mic
            is DeviceClass.AudioVideo.WearableHeadset -> R.drawable.ic_headset_mic
            is DeviceClass.AudioVideo.Loudspeaker -> R.drawable.ic_speaker
            is DeviceClass.AudioVideo.HifiAudio -> R.drawable.ic_speaker
            is DeviceClass.AudioVideo.CarAudio -> R.drawable.ic_car
            is DeviceClass.AudioVideo.VideoCamera -> R.drawable.ic_camera
            is DeviceClass.AudioVideo.VideoGamingToy -> R.drawable.ic_videogame
            is DeviceClass.AudioVideo -> R.drawable.ic_audio

            is DeviceClass.Wearable.WristWatch -> R.drawable.ic_watch
            is DeviceClass.Wearable.Helmet -> R.drawable.ic_helmet
            is DeviceClass.Wearable.Glasses -> R.drawable.ic_glasses
            is DeviceClass.Wearable -> R.drawable.ic_watch

            is DeviceClass.Toy -> R.drawable.ic_toy

            is DeviceClass.Health.Weighing -> R.drawable.ic_weight
            is DeviceClass.Health.Thermometer -> R.drawable.ic_thermometer
            is DeviceClass.Health -> R.drawable.ic_health_monitor

            is DeviceClass.Peripheral.Keyboard -> R.drawable.ic_keyboard
            is DeviceClass.Peripheral.Pointing -> R.drawable.ic_mouse
            is DeviceClass.Peripheral -> R.drawable.ic_joystick

            is DeviceClass.Beacon -> R.drawable.ic_ibeacon
            else -> R.drawable.ic_question
        }
    }
}
