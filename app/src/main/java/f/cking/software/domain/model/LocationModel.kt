package f.cking.software.domain.model

import android.location.Location
import org.osmdroid.util.GeoPoint
import java.io.Serializable

@kotlinx.serialization.Serializable
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

fun LocationModel.toLocation(): Location {
    val location = Location("")
    location.latitude = lat
    location.longitude = lng
    location.time = time
    return location
}

fun LocationModel.toGeoPoint(): GeoPoint {
    return GeoPoint(lat, lng)
}

fun Location.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}