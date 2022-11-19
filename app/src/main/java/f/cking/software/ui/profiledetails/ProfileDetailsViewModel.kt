package f.cking.software.ui.profiledetails

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.*
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class ProfileDetailsViewModel(
    private val radarProfilesRepository: RadarProfilesRepository,
    val router: NavRouter,
    val appContext: Application,
) : ViewModel() {

    var profileId: Optional<Int> by mutableStateOf(Optional.empty())

    var name: String by mutableStateOf("")
    var description: String by mutableStateOf("")
    var isActive: Boolean by mutableStateOf(true)
    var filter: Optional<UiFilterState> by mutableStateOf(Optional.empty())

    init {
        initProfile(profileId.orNull())
    }

    fun setId(id: Int?) {
        profileId = Optional.ofNullable(id)
        name = ""
        description = ""
        isActive = true
        filter = Optional.empty()
        initProfile(id)
    }

    private fun initProfile(id: Int?) {
        if (id != null) {
            viewModelScope.launch {
                val profile = radarProfilesRepository.getById(id = id)
                loadExisting(profile)
            }
        }
    }

    private fun loadExisting(profile: RadarProfile?) {
        if (profile != null) {
            name = profile.name
            description = profile.description.orEmpty()
            isActive = profile.isActive
            filter = Optional.ofNullable(profile.detectFilter?.let(::mapToUi))
        } else {
            back()
        }
    }

    fun onSaveClick() {
        if ((!filter.isPresent || filter.get().isCorrect()) && name.isNotBlank()) {
            saveProfile()
        } else {
            Toast.makeText(appContext, "Cannot save profile. Check your configuration", Toast.LENGTH_LONG).show()
        }
    }

    fun onRemoveClick() {
        if (profileId.isPresent) {
            viewModelScope.launch {
                radarProfilesRepository.deleteProfile(profileId.get())
                Toast.makeText(appContext, "'$name' profile removed", Toast.LENGTH_SHORT).show()
                back()
            }
        } else {
            back()
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            radarProfilesRepository.saveProfile(buildProfile())
            router.navigate(BackCommand)
        }
    }

    private fun buildProfile(): RadarProfile {
        return RadarProfile(
            id = profileId.orNull(),
            name = name,
            description = description,
            isActive = isActive,
            detectFilter = filter.orNull()?.let(::mapToDomain),
        )
    }

    fun back() {
        router.navigate(BackCommand)
    }

    private fun mapToDomain(from: UiFilterState): RadarProfile.Filter {
        return when (from) {
            is UiFilterState.Name -> RadarProfile.Filter.Name(from.name, from.ignoreCase)
            is UiFilterState.Address -> RadarProfile.Filter.Address(from.address)
            is UiFilterState.IsFavorite -> RadarProfile.Filter.IsFavorite(from.favorite)
            is UiFilterState.Manufacturer -> RadarProfile.Filter.Manufacturer(from.manufacturer.get().id)
            is UiFilterState.Any -> RadarProfile.Filter.Any(from.filters.map { mapToDomain(it) })
            is UiFilterState.All -> RadarProfile.Filter.All(from.filters.map { mapToDomain(it) })
            is UiFilterState.Not -> RadarProfile.Filter.Not(mapToDomain(from.filter.get()))
            is UiFilterState.LastDetectionInterval -> RadarProfile.Filter.LastDetectionInterval(
                from = if (from.fromDate.isPresent && from.fromTime.isPresent) {
                    timeFromDateTime(from.fromDate.get(), from.fromTime.get())
                } else {
                    Long.MIN_VALUE
                },
                to = if (from.toDate.isPresent && from.toTime.isPresent) {
                    timeFromDateTime(from.toDate.get(), from.toTime.get())
                } else {
                    Long.MAX_VALUE
                }
            )
            is UiFilterState.FirstDetectionInterval -> RadarProfile.Filter.FirstDetectionInterval(
                from = if (from.fromDate.isPresent && from.fromTime.isPresent) {
                    timeFromDateTime(from.fromDate.get(), from.fromTime.get())
                } else {
                    Long.MIN_VALUE
                },
                to = if (from.toDate.isPresent && from.toTime.isPresent) {
                    timeFromDateTime(from.toDate.get(), from.toTime.get())
                } else {
                    Long.MAX_VALUE
                }
            )
            is UiFilterState.MinLostTime -> RadarProfile.Filter.MinLostTime(from.minLostTime.get().toMilliseconds())
            is UiFilterState.AppleAirdropContact -> RadarProfile.Filter.AppleAirdropContact(
                contactStr = from.contactString.trim(),
                airdropShaFormat = SHA256.fromStringAirdrop(from.contactString),
            )
            is UiFilterState.Unknown, is UiFilterState.Interval -> throw IllegalArgumentException("Unsupported type: ${from::class.java}")
        }
    }

    private fun mapToUi(from: RadarProfile.Filter): UiFilterState {
        return when (from) {
            is RadarProfile.Filter.Name -> UiFilterState.Name().apply {
                this.name = from.name
                this.ignoreCase = from.ignoreCase
            }
            is RadarProfile.Filter.Address -> UiFilterState.Address().apply {
                this.address = from.address
            }
            is RadarProfile.Filter.Manufacturer -> UiFilterState.Manufacturer().apply {
                this.manufacturer =
                    BluetoothSIG.bluetoothSIG[from.manufacturerId]?.let {
                        Optional.of(
                            ManufacturerInfo(
                                from.manufacturerId,
                                it,
                                null,
                            )
                        )
                    }
                        ?: Optional.empty()
            }
            is RadarProfile.Filter.IsFavorite -> UiFilterState.IsFavorite().apply {
                this.favorite = from.favorite
            }
            is RadarProfile.Filter.FirstDetectionInterval -> UiFilterState.FirstDetectionInterval().apply {
                this.fromDate =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalDate()) else Optional.empty()
                this.fromTime =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalTime()) else Optional.empty()
                this.toDate =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalDate()) else Optional.empty()
                this.toTime =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalTime()) else Optional.empty()
            }
            is RadarProfile.Filter.LastDetectionInterval -> UiFilterState.LastDetectionInterval().apply {
                this.fromDate =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalDate()) else Optional.empty()
                this.fromTime =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalTime()) else Optional.empty()
                this.toDate =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalDate()) else Optional.empty()
                this.toTime =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalTime()) else Optional.empty()
            }
            is RadarProfile.Filter.MinLostTime -> UiFilterState.MinLostTime().apply {
                this.minLostTime = Optional.of(from.minLostTime.toLocalTime())
            }
            is RadarProfile.Filter.All -> UiFilterState.All().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Any -> UiFilterState.Any().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Not -> UiFilterState.Not().apply {
                this.filter = Optional.of(mapToUi(from.filter))
            }
            is RadarProfile.Filter.AppleAirdropContact -> UiFilterState.AppleAirdropContact().apply {
                this.contactString = from.contactStr
            }
        }
    }

    sealed class UiFilterState {

        abstract fun isCorrect(): Boolean

        abstract class Interval : UiFilterState() {
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

        class Name : UiFilterState() {
            var name: String by mutableStateOf("")
            var ignoreCase: Boolean by mutableStateOf(true)

            override fun isCorrect(): Boolean {
                return true
            }
        }

        class Address : UiFilterState() {
            var address: String by mutableStateOf("")

            override fun isCorrect(): Boolean {
                return address.isNotBlank()
            }
        }

        class Manufacturer : UiFilterState() {
            var manufacturer: Optional<ManufacturerInfo> by mutableStateOf(Optional.empty())

            override fun isCorrect(): Boolean {
                return manufacturer.isPresent
            }
        }

        class IsFavorite : UiFilterState() {
            var favorite: Boolean by mutableStateOf(true)

            override fun isCorrect(): Boolean {
                return true
            }
        }

        class MinLostTime : UiFilterState() {
            var minLostTime: Optional<LocalTime> by mutableStateOf(Optional.empty())

            override fun isCorrect(): Boolean {
                return minLostTime.isPresent
            }
        }

        class Any : UiFilterState() {
            var filters: List<UiFilterState> by mutableStateOf(emptyList())

            fun delete(filter: UiFilterState) {
                filters = filters.filter { it !== filter }
            }

            override fun isCorrect(): Boolean {
                return filters.isNotEmpty() && filters.all { it.isCorrect() }
            }
        }

        class All : UiFilterState() {
            var filters: List<UiFilterState> by mutableStateOf(emptyList())

            fun delete(filter: UiFilterState) {
                filters = filters.filter { it !== filter }
            }

            override fun isCorrect(): Boolean {
                return filters.isNotEmpty() && filters.all { it.isCorrect() }
            }
        }

        class Not : UiFilterState() {
            var filter: Optional<UiFilterState> by mutableStateOf(Optional.empty())

            fun delete(filter: UiFilterState) {
                this.filter = Optional.empty()
            }

            override fun isCorrect(): Boolean {
                return filter.isPresent && filter.get().isCorrect()
            }
        }

        class AppleAirdropContact() : UiFilterState() {
            var contactString: String by mutableStateOf("")

            override fun isCorrect(): Boolean {
                return contactString.isNotBlank()
            }
        }

        class Unknown : UiFilterState() {
            override fun isCorrect(): Boolean {
                return false
            }
        }
    }
}