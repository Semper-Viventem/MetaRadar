package f.cking.software.domain.interactor

import f.cking.software.concatTwoBytes
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.BleRecordFrame

class GetAirdropInfoFromBleFrame {

    fun execute(frame: BleRecordFrame): AppleAirDrop? {
        val isAirDropPackage = frame.data.size == APPLE_AIRDROP_PACKAGE_SIZE
                && frame.data[2] == APPLE_AIRDROP_PACKAGE_TYPE
        return if (isAirDropPackage) {

            val payload = ByteArray(APPLE_AIRDROP_PACKAGE_SIZE)
            System.arraycopy(frame.data, 3, payload, 0, APPLE_AIRDROP_PAYLOAD_SIZE)

            getContactsFromAirdropPayload(payload)
                .takeIf { it.isNotEmpty() }
                ?.let { AppleAirDrop(it) }
        } else {
            null
        }
    }

    private fun getContactsFromAirdropPayload(
        payload: ByteArray,
    ): List<AppleAirDrop.AppleContact> {
        return listOf(
            AppleAirDrop.AppleContact(concatTwoBytes(payload[10], payload[11])),
            AppleAirDrop.AppleContact(concatTwoBytes(payload[12], payload[13])),
            AppleAirDrop.AppleContact(concatTwoBytes(payload[14], payload[15])),
            AppleAirDrop.AppleContact(concatTwoBytes(payload[16], payload[17])),
        )
    }

    companion object {
        private const val APPLE_AIRDROP_PACKAGE_TYPE = 0x05.toByte()
        private const val APPLE_AIRDROP_PACKAGE_SIZE = 22
        private const val APPLE_AIRDROP_PAYLOAD_SIZE = 19
    }
}