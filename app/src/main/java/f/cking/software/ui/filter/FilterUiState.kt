package f.cking.software.ui.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import f.cking.software.TheAppConfig
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.toLocalTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

sealed class FilterUiState {

    abstract fun isCorrect(): Boolean

    abstract class Interval : FilterUiState() {
        var fromDate: Optional<LocalDate> by mutableStateOf(Optional.empty())
        var fromTime: Optional<LocalTime> by mutableStateOf(Optional.empty())
        var toDate: Optional<LocalDate> by mutableStateOf(Optional.empty())
        var toTime: Optional<LocalTime> by mutableStateOf(Optional.empty())

        override fun isCorrect(): Boolean {
            return (fromDate.isPresent && fromTime.isPresent) || (toDate.isPresent && toTime.isPresent)
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
        var manufacturer: Optional<ManufacturerInfo> by mutableStateOf(Optional.empty())

        override fun isCorrect(): Boolean {
            return manufacturer.isPresent
        }
    }

    class IsFavorite : FilterUiState() {
        var favorite: Boolean by mutableStateOf(true)

        override fun isCorrect(): Boolean {
            return true
        }
    }

    class MinLostTime : FilterUiState() {
        var minLostTime: Optional<LocalTime> by mutableStateOf(Optional.empty())

        override fun isCorrect(): Boolean {
            return minLostTime.isPresent
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
        var filter: Optional<FilterUiState> by mutableStateOf(Optional.empty())

        fun delete(filter: FilterUiState) {
            this.filter = Optional.empty()
        }

        override fun isCorrect(): Boolean {
            return filter.isPresent && filter.get().isCorrect()
        }
    }

    class AppleAirdropContact() : FilterUiState() {
        var contactString: String by mutableStateOf("")
        var minLostTime: Optional<LocalTime> by mutableStateOf(Optional.empty())

        override fun isCorrect(): Boolean {
            return contactString.isNotBlank()
        }
    }

    class IsFollowing() : FilterUiState() {
        var followingDurationMs: LocalTime by mutableStateOf(TheAppConfig.MIN_FOLLOWING_DURATION_TIME_MS.toLocalTime())
        var followingDetectionIntervalMs: LocalTime by mutableStateOf(TheAppConfig.MIN_FOLLOWING_INTERVAL_TIME_MS.toLocalTime())

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