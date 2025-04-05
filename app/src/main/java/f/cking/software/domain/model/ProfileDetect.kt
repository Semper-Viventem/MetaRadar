package f.cking.software.domain.model

data class ProfileDetect(
    val id: Int?,
    val profileId: Int,
    val triggerTime: Long,
    val deviceAddress: String,
)
