package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.location.LocationListenerCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocationProvider(
    private val context: Context
) {

    private val TAG = "LocationProvider"

    private val locationState = MutableStateFlow<LocationHandle?>(null)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val locationListener = LocationListenerCompat {
        Log.d(TAG, "New location: lat=${it.latitude}, lng=${it.longitude}")
        locationState.tryEmit(LocationHandle(it, System.currentTimeMillis()))
    }

    fun isLocationAvailable(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }

    fun observeLocation(): Flow<LocationHandle?> {
        return locationState
    }

    @SuppressLint("MissingPermission")
    fun lastKnownLocation(): LocationHandle? {
        return locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?.let { LocationHandle(it, System.currentTimeMillis()) }
    }

    @SuppressLint("MissingPermission")
    fun startLocationLeastening() {
        if (!isLocationAvailable() || locationManager == null) {
            throw LocationManagerIsNotAvailableException()
        }
        val isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (isGpsProviderEnabled) {
            locationState.tryEmit(lastKnownLocation())

            val provider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                LocationManager.FUSED_PROVIDER
            } else {
                LocationManager.GPS_PROVIDER
            }

            locationManager.requestLocationUpdates(
                provider,
                INTERVAL_MS,
                SMALLEST_DISPLACEMENT_METERS,
                locationListener,
                Looper.getMainLooper()
            )
        } else {
            throw GpsProviderIsNotEnabledException()
        }
    }

    fun stopLocationListening() {
        locationManager?.removeUpdates(locationListener)
    }

    data class LocationHandle(
        val location: Location,
        val emitTime: Long,
    )

    class LocationManagerIsNotAvailableException :
        IllegalStateException("Location manager is not available for this device")

    class GpsProviderIsNotEnabledException : IllegalStateException("GPS provider is not enabled")

    companion object {
        private const val SMALLEST_DISPLACEMENT_METERS = 5f
        private const val INTERVAL_MS = 10_000L
    }
}