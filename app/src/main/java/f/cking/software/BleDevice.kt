package f.cking.software

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

data class BleDevice(
    val address: String,
    val name: String?,
    val bondState: Int,
    val device: BluetoothDevice,
    val state: State?,
) {
    sealed class State(val name: String) {
        data class Connecting(val gatt: BluetoothGatt) : State("connecting")
        data class Connected(val gatt: BluetoothGatt, val characteristics: List<BluetoothGattCharacteristic>?) :
            State("connected")

        data class Disconnecting(val gatt: BluetoothGatt) : State("disconnecting")
        data class Disconnected(val gatt: BluetoothGatt) : State("disconnected")
    }
}