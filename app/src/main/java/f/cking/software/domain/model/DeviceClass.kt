package f.cking.software.domain.model

sealed interface DeviceClass {
    data object Unknown : DeviceClass

    sealed interface Phone : DeviceClass {
        data object Uncategorised : Phone

        data object Smartphone : Phone

        data object Cellular : Phone

        data object ModemOrGateway : Phone

        data object Isdn : Phone

        data object Cordless : Phone
    }

    sealed interface Computer : DeviceClass {
        data object Uncategorised : Computer

        data object Laptop : Computer

        data object Desktop : Computer

        data object Server : Computer

        data object Wearable : Computer
    }

    sealed interface AudioVideo : DeviceClass {
        data object Uncategorised : AudioVideo

        data object WearableHeadset : AudioVideo

        data object HandsFree : AudioVideo

        data object Microphone : AudioVideo

        data object Loudspeaker : AudioVideo

        data object Headphones : AudioVideo

        data object PortableAudio : AudioVideo

        data object CarAudio : AudioVideo

        data object SetTopBox : AudioVideo

        data object HifiAudio : AudioVideo

        data object Vcr : AudioVideo

        data object VideoCamera : AudioVideo

        data object Camcorder : AudioVideo

        data object VideoMonitor : AudioVideo

        data object VideoDisplayAndLoudspeaker : AudioVideo

        data object VideoConferencing : AudioVideo

        data object VideoGamingToy : AudioVideo
    }

    sealed interface Wearable : DeviceClass {
        data object Uncategorised : Wearable

        data object WristWatch : Wearable

        data object Pager : Wearable

        data object Jacket : Wearable

        data object Helmet : Wearable

        data object Glasses : Wearable
    }

    sealed interface Toy : DeviceClass {
        data object Uncategorised : Toy

        data object Robot : Toy

        data object Vehicle : Toy

        data object Doll : Toy

        data object Controller : Toy

        data object Game : Toy
    }

    sealed interface Health : DeviceClass {
        data object Uncategorised : Health

        data object BloodPressure : Health

        data object Thermometer : Health

        data object Weighing : Health

        data object Glucose : Health

        data object PulseOximeter : Health

        data object HeartPulseRate : Health

        data object HealthDataDisplay : Health
    }

    sealed interface Peripheral : DeviceClass {
        data object Uncategorised : Peripheral

        data object Keyboard : Peripheral

        data object Pointing : Peripheral

        data object KeyboardPointing : Peripheral
    }

    sealed interface Beacon : DeviceClass {
        data object AirTag : Beacon

        data object IBeacon : Beacon

        data object SmartTag : Beacon

        data object Uncategorised : Beacon
    }
}
