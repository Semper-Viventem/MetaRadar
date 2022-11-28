package f.cking.software.domain.model

import java.io.Serializable

data class LocationModel(
    val lat: Double,
    val lng: Double,
) : Serializable