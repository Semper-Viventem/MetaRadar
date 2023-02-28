package f.cking.software.ui.profileslist

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.R
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.filter.FilterUiState
import kotlinx.coroutines.launch

class ProfilesListViewModel(
    private val radarProfilesRepository: RadarProfilesRepository,
    private val router: NavRouter,
) : ViewModel() {

    var profiles: List<RadarProfile> by mutableStateOf(emptyList())
    var defaultFiltersTemplate: List<FilterTemplate> by mutableStateOf(listOf(DEFAULT_FILTER_FOLLOWING, DEFAULT_FILTER_FAVORITE))

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            radarProfilesRepository.observeAllProfiles()
                .collect { profiles = it }
        }
    }

    fun createNewClick() {
        router.navigate(ScreenNavigationCommands.OpenProfileScreen(profileId = null, template = null))
    }

    fun selectFilterTemplate(template: FilterTemplate) {
        router.navigate(ScreenNavigationCommands.OpenProfileScreen(profileId = null, template = template.filterUiState))
    }

    fun onProfileClick(profile: RadarProfile) {
        router.navigate(ScreenNavigationCommands.OpenProfileScreen(profile.id, template = null))
    }

    data class FilterTemplate(
        val filterUiState: FilterUiState,
        @StringRes val displayNameRes: Int,
    )

    companion object {
        private val DEFAULT_FILTER_FOLLOWING = FilterTemplate(
            displayNameRes = R.string.filter_device_is_following_me,
            filterUiState = FilterUiState.All().apply {
                filters = listOf(FilterUiState.IsFollowing())
            }
        )
        private val DEFAULT_FILTER_FAVORITE = FilterTemplate(
            displayNameRes = R.string.is_favorite,
            filterUiState = FilterUiState.All().apply {
                filters = listOf(FilterUiState.IsFavorite())
            }
        )
    }
}