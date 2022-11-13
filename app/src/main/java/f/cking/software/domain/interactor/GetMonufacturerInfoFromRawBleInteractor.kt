package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleRecordFrame
import f.cking.software.domain.model.BluetoothSIG
import f.cking.software.domain.model.ManufacturerInfo

class GetMonufacturerInfoFromRawBleInteractor(
    private val getBleRecordFramesFromRawInteractor: GetBleRecordFramesFromRawInteractor
) {

    fun execute(raw: ByteArray): ManufacturerInfo? {
        val frame: BleRecordFrame = getBleRecordFramesFromRawInteractor.execute(raw).firstOrNull {
            it.type == TYPE_MANUFACTURER_INFO
        } ?: return null

        return frame.data.takeIf { it.count() >= MIN_MANUFACTURER_ID_LENGTH }
            ?.let { data ->
                val id = 0 or (data[1].toInt() shl 8) or data[0].toInt()
                BluetoothSIG.bluetoothSIG[id]?.let { name -> ManufacturerInfo(id, name) }
            }
    }

    companion object {
        private const val MIN_MANUFACTURER_ID_LENGTH = 2
        private const val TYPE_MANUFACTURER_INFO: Byte = 0xFF.toByte()
    }
}