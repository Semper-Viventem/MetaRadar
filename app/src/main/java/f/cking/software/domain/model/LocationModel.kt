package f.cking.software.domain.model

import android.location.Location
import java.io.Serializable

data class LocationModel(
    val lat: Double,
    val lng: Double,
    val time: Long,
) : Serializable {

    fun distanceTo(other: LocationModel): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat, lng, other.lat, other.lng, result)
        return result[0]
    }
}