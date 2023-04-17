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
import f.cking.software.domain.interactor.DeleteRadarProfile
import f.cking.software.domain.interactor.SaveRadarProfile
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.filter.FilterUiMapper
import f.cking.software.ui.filter.FilterUiState
import kotlinx.coroutines.launch

class ProfileDetailsViewModel(
    val profileId: Int?,
    val template: FilterUiState?,
    val router: NavRouter,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val saveRadarProfile: SaveRadarProfile,
    private val deleteRadarProfile: DeleteRadarProfile,
    private val context: Application,
) : ViewModel() {

    var originalProfile: RadarProfile? = null
    var name: String by mutableStateOf("")
    var description: String by mutableStateOf("")
    var isActive: Boolean by mutableStateOf(true)
    var filter: FilterUiState? by mutableStateOf(template)

    init {
        if (profileId != null) {
            loadProfile(profileId)
        } else {
            handleProfile(EMPTY_PROFILE, template = template)
        }
    }

    fun onIsActiveClick() {
        isActive = !isActive
    }

    fun checkUnsavedChanges(): Boolean {
        return (originalProfile != null && originalProfile != buildProfile()) || (originalProfile == null && buildProfile() != EMPTY_PROFILE)
    }

    fun back() {
        router.navigate(BackCommand)
    }

    fun onSaveClick() {
        if (filter?.isCorrect() == true && name.isNotBlank() && buildProfile() != null) {
            viewModelScope.launch {
                saveRadarProfile.execute(buildProfile()!!)
                router.navigate(BackCommand)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.cannot_save_profile), Toast.LENGTH_LONG).show()
        }
    }

    fun onRemoveClick() {
        if (profileId != null) {
            viewModelScope.launch {
                deleteRadarProfile.execute(profileId)
                Toast.makeText(context, context.getString(R.string.profile_has_been_removed), Toast.LENGTH_SHORT).show()
                back()
            }
        } else {
            back()
        }
    }

    private fun loadProfile(id: Int) {
        viewModelScope.launch {
            val profile = radarProfilesRepository.getById(id = id)
            originalProfile = profile
            if (profile != null) {
                handleProfile(profile, template = null)
            } else {
                back()
            }
        }
    }

    private fun handleProfile(profile: RadarProfile, template: FilterUiState?) {
        name = profile.name
        description = profile.description.orEmpty()
        isActive = profile.isActive
        filter = template ?: profile.detectFilter?.let(FilterUiMapper::mapToUi)
    }

    private fun buildProfile(): RadarProfile? {
        return try {
            RadarProfile(
                id = profileId,
                name = name,
                description = description,
                isActive = isActive,
                detectFilter = filter?.let(FilterUiMapper::mapToDomain),
            )
        } catch (e: Throwable) {
            null
        }
    }

    companion object {
        private val EMPTY_PROFILE = RadarProfile(
            id = null,
            name = "",
            description = "",
            isActive = true,
            detectFilter = null,
        )
    }
}