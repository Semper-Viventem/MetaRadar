package f.cking.software

data class BleDevice(
    val address: String,
    val name: String?,
    val bondState: Int,
    val scanTimeMs: Long,
)