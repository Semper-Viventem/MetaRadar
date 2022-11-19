package f.cking.software.domain.interactor

import f.cking.software.concatTwoBytes
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.BleRecordFrame
import f.cking.software.domain.model.ManufacturerInfo

class GetManufacturerInfoFromRawBleInteractor(
    private val getBleRecordFramesFromRawInteractor: GetBleRecordFramesFromRawInteractor,
    private val getAirdropInfoFromBleFrame: GetAirdropInfoFromBleFrame,
) {

    fun execute(raw: ByteArray, detectionTimeMs: Long): ManufacturerInfo? {
        val frame: BleRecordFrame = getBleRecordFramesFromRawInteractor.execute(raw).firstOrNull {
            it.type == TYPE_MANUFACTURER_INFO
        } ?: return null

        return frame.data.takeIf { it.count() >= MIN_MANUFACTURER_ID_LENGTH }
            ?.let { data ->
                val id = decodeId(data[0], data[1])
                BluetoothSIG.bluetoothSIG[id]?.let { name ->
                    ManufacturerInfo(id, name, checkAirdrop(frame, id, detectionTimeMs))
                }
            }
    }

    private fun checkAirdrop(
        frame: BleRecordFrame,
        manufacturerId: Int,
        detectionTimeMs: Long,
    ): AppleAirDrop? {
        return if (manufacturerId == ManufacturerInfo.APPLE_ID) {
            getAirdropInfoFromBleFrame.execute(frame, detectionTimeMs)
        } else {
            null
        }
    }

    private fun decodeId(firstByte: Byte, secondByte: Byte): Int {
        val encoded: Int = concatTwoBytes(firstByte, secondByte)
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