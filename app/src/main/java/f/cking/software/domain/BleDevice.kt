package f.cking.software.domain

data class BleDevice(
    val address: String,
    val name: String?,
    val bondState: Int,
    val scanTimeMs: Long,
)