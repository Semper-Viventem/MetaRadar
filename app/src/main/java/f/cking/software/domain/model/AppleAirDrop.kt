package f.cking.software.domain.model

data class AppleAirDrop(
    val contacts: List<AppleContact>
) {
    /**
     * We cannot specify the contact type (phone/email) because apple shuffles it in airdrop packages
     */
    data class AppleContact(
        val sha256: Int, // only first 2 bytes of sha(contact)
        val lastDetectionTimeMs: Long,
        val firstDetectionTimeMs: Long,
    ) {
        fun mergeWithNewContact(new: AppleContact): AppleContact {
            return new.copy(firstDetectionTimeMs = firstDetectionTimeMs)
        }
    }
}