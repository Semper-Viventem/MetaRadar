package f.cking.software.domain.model

data class BleScanDevice(
    val address: String,
    val name: String?,
    val bondState: Int,
    val scanTimeMs: Long,
)