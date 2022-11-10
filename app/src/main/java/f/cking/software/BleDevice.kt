package f.cking.software

import android.bluetooth.BluetoothDevice

data class BleDevice(
    val address: String,
    val name: String?,
    val bondState: Int,
    val device: BluetoothDevice,
)