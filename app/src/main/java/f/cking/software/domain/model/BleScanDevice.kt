package f.cking.software.domain.model

class BleScanDevice(
    val address: String,
    val name: String?,
    val scanTimeMs: Long,
    val scanRecordRaw: ByteArray?,
)