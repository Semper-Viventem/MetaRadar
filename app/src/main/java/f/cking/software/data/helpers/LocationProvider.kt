package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationListenerCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class LocationProvider(
    private val context: Context
) {

    private val locationState = MutableStateFlow<Location?>(null)

    private val locationManager: LocationManager? = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val locationListener = LocationListenerCompat {
        locationState.tryEmit(it)
    }

    suspend fun isLocationAvailable(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }

    suspend fun observeLocation(): Flow<Location> {
        return locationState.filterNotNull().apply {
            startLocationFeating()
        }
    }

    @SuppressLint("MissingPermission")
    fun lastKnownLocation(): Location? =
        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    suspend fun startLocationFeating() {
        if (!isLocationAvailable() || locationManager == null) {
            throw LocationManagerIsNotAvailableException()
        }
        val isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)


        if (isGpsProviderEnabled) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                INTERVAL_MS,
                SMALLEST_DISPLACEMENT_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        } else {
            throw GpsProviderIsNotEnabledException()
        }
    }


    class LocationManagerIsNotAvailableException :
        IllegalStateException("Location manager is not available for this device")

    class GpsProviderIsNotEnabledException : IllegalStateException("GPS provider is not enabled")

    companion object {
        private const val SMALLEST_DISPLACEMENT_METERS = 5f
        private const val INTERVAL_MS = 10_000L
    }
}