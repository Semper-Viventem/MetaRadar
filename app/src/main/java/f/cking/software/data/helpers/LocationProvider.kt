package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.location.LocationListenerCompat
import f.cking.software.data.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull

class LocationProvider(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val TAG = "LocationProvider"

    private val locationState = MutableStateFlow<LocationHandle?>(null)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val locationListener = LocationListenerCompat {
        Log.d(TAG, "New location: lat=${it.latitude}, lng=${it.longitude}")
        locationState.tryEmit(LocationHandle(it, System.currentTimeMillis()))
    }

    private var isActive: Boolean = false

    fun isLocationAvailable(): Boolean {
        return (locationManager?.isProviderEnabled(provider())
            ?: false) && (locationManager?.isLocationEnabled ?: false)
    }

    fun isActive(): Boolean {
        return isActive
    }

    fun observeLocation(): Flow<LocationHandle?> {
        return locationState
    }

    suspend fun getFreshLocation(): Location? {
        return observeLocation()
            .firstOrNull()
            ?.takeIf { it.isFresh() }
            ?.location
    }

    @SuppressLint("MissingPermission")
    fun startLocationLeastening() {
        if (!isLocationAvailable() || locationManager == null) {
            throw LocationManagerIsNotAvailableException()
        }
        val isGpsProviderEnabled = locationManager.isProviderEnabled(provider())

        if (isGpsProviderEnabled) {
            locationManager.requestLocationUpdates(
                provider(),
                INTERVAL_MS,
                SMALLEST_DISPLACEMENT_METERS,
                locationListener,
            )
            isActive = true
        } else {
            throw GpsProviderIsNotEnabledException()
        }
    }

    fun stopLocationListening() {
        locationManager?.removeUpdates(locationListener)
        isActive = false
    }

    private fun provider(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !settingsRepository.getUseGpsLocationOnly()) {
            LocationManager.FUSED_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER
        }
    }

    private fun LocationHandle.isFresh(): Boolean {
        return System.currentTimeMillis() - this.emitTime < ALLOWED_LOCATION_LIVETIME_MS
    }

    data class LocationHandle(
        val location: Location,
        val emitTime: Long,
    )

    class LocationManagerIsNotAvailableException :
        IllegalStateException("Location manager is not available for this device")

    class GpsProviderIsNotEnabledException : IllegalStateException("GPS provider is not enabled")

    companion object {
        private const val SMALLEST_DISPLACEMENT_METERS = 3f
        private const val INTERVAL_MS = 10_000L
        private const val ALLOWED_LOCATION_LIVETIME_MS = 2L * 60L * 1000L // 2 min
    }
}