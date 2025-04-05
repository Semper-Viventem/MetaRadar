package f.cking.software.domain.model

class BleScanDevice(
    val address: String,
    val name: String?,
    val scanTimeMs: Long,
    val scanRecordRaw: ByteArray?,
    val rssi: Int?,
    val addressType: Int?,
    val deviceClass: Int?,
    val isPaired: Boolean,
    val serviceUuids: List<String>,
    val isConnectable: Boolean,
)
