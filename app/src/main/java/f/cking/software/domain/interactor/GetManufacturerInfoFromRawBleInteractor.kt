package f.cking.software.domain.interactor

import f.cking.software.domain.helpers.BluetoothSIG
import f.cking.software.domain.model.BleRecordFrame
import f.cking.software.domain.model.ManufacturerInfo

class GetManufacturerInfoFromRawBleInteractor(
    private val getBleRecordFramesFromRawInteractor: GetBleRecordFramesFromRawInteractor
) {

    fun execute(raw: ByteArray): ManufacturerInfo? {
        val frame: BleRecordFrame = getBleRecordFramesFromRawInteractor.execute(raw).firstOrNull {
            it.type == TYPE_MANUFACTURER_INFO
        } ?: return null

        return frame.data.takeIf { it.count() >= MIN_MANUFACTURER_ID_LENGTH }
            ?.let { data ->
                val id = decodeId(data[0], data[1])
                BluetoothSIG.bluetoothSIG[id]?.let { name -> ManufacturerInfo(id, name) }
            }
    }

    private fun decodeId(firstByte: Byte, secondByte: Byte): Int {
        val encoded: Int = (firstByte.toInt() shl 8) or secondByte.toInt()
        val shiftValue = when {
            encoded and 0x0fff == 0x0 -> 12
            encoded and 0x00ff == 0x0 -> 8
            encoded and 0x000f == 0x0 -> 4
            else -> 0
        }
        return encoded shr shiftValue
    }

    companion object {
        private const val MIN_MANUFACTURER_ID_LENGTH = 2
        private const val TYPE_MANUFACTURER_INFO: Byte = 0xFF.toByte()
    }
}