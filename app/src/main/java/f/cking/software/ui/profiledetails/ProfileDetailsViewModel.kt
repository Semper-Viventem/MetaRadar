package f.cking.software.ui.profiledetails

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.RadarProfile
import f.cking.software.orNull
import f.cking.software.ui.devicelist.FilterUiMapper
import f.cking.software.ui.filter.FilterUiState
import kotlinx.coroutines.launch
import java.util.*

class ProfileDetailsViewModel(
    private val radarProfilesRepository: RadarProfilesRepository,
    val router: NavRouter,
    val context: Application,
) : ViewModel() {

    var profileId: Optional<Int> by mutableStateOf(Optional.empty())

    var name: String by mutableStateOf("")
    var description: String by mutableStateOf("")
    var isActive: Boolean by mutableStateOf(true)
    var filter: FilterUiState? by mutableStateOf(null)

    init {
        initProfile(profileId.orNull())
    }

    fun setId(id: Int?) {
        profileId = Optional.ofNullable(id)
        name = ""
        description = ""
        isActive = true
        filter = null
        initProfile(id)
    }

    fun onIsActiveClick() {
        isActive = !isActive
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
            filter = profile.detectFilter?.let(FilterUiMapper::mapToUi)
        } else {
            back()
        }
    }

    fun onSaveClick() {
        if (filter?.isCorrect() == true && name.isNotBlank()) {
            saveProfile()
        } else {
            Toast.makeText(context, context.getString(R.string.cannot_save_profile), Toast.LENGTH_LONG).show()
        }
    }

    fun onRemoveClick() {
        if (profileId.isPresent) {
            viewModelScope.launch {
                radarProfilesRepository.deleteProfile(profileId.get())
                Toast.makeText(context, context.getString(R.string.profile_has_been_removed), Toast.LENGTH_SHORT).show()
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
            detectFilter = filter?.let(FilterUiMapper::mapToDomain),
        )
    }

    fun back() {
        router.navigate(BackCommand)
    }
}