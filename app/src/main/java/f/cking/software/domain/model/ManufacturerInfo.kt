package f.cking.software.domain.model

/**
 * Apple shuffles BLE addresses each 15 minutes but we can use airdrop contacts fingerprint to distinguish them
 */
data class ManufacturerInfo(
    val id: Int,
    val name: String,
    var airdrop: AppleAirDrop?,
) {
    companion object {
        const val APPLE_ID = 0x004C
    }
}