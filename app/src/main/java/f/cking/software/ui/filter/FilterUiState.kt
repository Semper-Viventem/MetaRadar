package f.cking.software.ui.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import f.cking.software.TheAppConfig
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.model.ManufacturerInfo
import java.time.LocalDate
import java.time.LocalTime

sealed class FilterUiState {

    abstract fun isCorrect(): Boolean

    abstract class Interval : FilterUiState() {
        var fromDate: LocalDate? by mutableStateOf(null)
        var fromTime: LocalTime? by mutableStateOf(null)
        var toDate: LocalDate? by mutableStateOf(null)
        var toTime: LocalTime? by mutableStateOf(null)

        open override fun isCorrect(): Boolean {
            return (fromDate != null && fromTime != null) || (toDate != null && toTime != null)
        }
    }

    class LastDetectionInterval : Interval()

    class FirstDetectionInterval : Interval()

    class Name : FilterUiState() {
        var name: String by mutableStateOf("")
        var ignoreCase: Boolean by mutableStateOf(true)

        override fun isCorrect(): Boolean {
            return true
        }
    }

    class Address : FilterUiState() {
        var address: String by mutableStateOf("")

        override fun isCorrect(): Boolean {
            return address.isNotBlank()
        }
    }

    class Manufacturer : FilterUiState() {
        var manufacturer: ManufacturerInfo? by mutableStateOf(null)

        override fun isCorrect(): Boolean {
            return manufacturer != null
        }
    }

    class IsFavorite : FilterUiState() {
        var favorite: Boolean by mutableStateOf(true)

        override fun isCorrect(): Boolean {
            return true
        }
    }

    class MinLostTime : FilterUiState() {
        var minLostTime: Long? by mutableStateOf(null)

        override fun isCorrect(): Boolean {
            return minLostTime != null
        }
    }

    class Any : FilterUiState() {
        var filters: List<FilterUiState> by mutableStateOf(emptyList())

        fun delete(filter: FilterUiState) {
            filters = filters.filter { it !== filter }
        }

        override fun isCorrect(): Boolean {
            return filters.isNotEmpty() && filters.all { it.isCorrect() }
        }
    }

    class All : FilterUiState() {
        var filters: List<FilterUiState> by mutableStateOf(emptyList())

        fun delete(filter: FilterUiState) {
            filters = filters.filter { it !== filter }
        }

        override fun isCorrect(): Boolean {
            return filters.isNotEmpty() && filters.all { it.isCorrect() }
        }
    }

    class Not : FilterUiState() {
        var filter: FilterUiState? by mutableStateOf(null)

        fun delete(filter: FilterUiState) {
            this.filter = null
        }

        override fun isCorrect(): Boolean {
            return filter != null && filter!!.isCorrect()
        }
    }

    class AppleAirdropContact : FilterUiState() {
        var contactString: String by mutableStateOf("")
        var minLostTime: Long? by mutableStateOf(null)

        override fun isCorrect(): Boolean {
            return contactString.isNotBlank()
        }
    }

    class DeviceLocation : Interval() {
        var targetLocation: LocationModel? by mutableStateOf(null)
        var radius: Float by mutableStateOf(TheAppConfig.DEFAULT_LOCATION_FILTER_RADIUS)

        override fun isCorrect(): Boolean {
            return targetLocation != null
                    && ((fromDate != null && fromTime != null) || (fromDate == null && fromTime == null))
                    && ((toDate != null && toTime != null) || (toDate == null && toTime == null))
        }
    }
    class IsFollowing() : FilterUiState() {
        var followingDurationMs: Long by mutableStateOf(TheAppConfig.MIN_FOLLOWING_DURATION_TIME_MS)
        var followingDetectionIntervalMs: Long by mutableStateOf(TheAppConfig.MIN_FOLLOWING_INTERVAL_TIME_MS)

        override fun isCorrect(): Boolean {
            return true
        }
    }

    class Unknown : FilterUiState() {
        override fun isCorrect(): Boolean {
            return false
        }
    }
}