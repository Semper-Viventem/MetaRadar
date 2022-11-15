package f.cking.software.ui.profileslist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.common.navigation.NavRouter
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.ScreenNavigationCommands
import kotlinx.coroutines.launch

class ProfilesListViewModel(
    private val radarProfilesRepository: RadarProfilesRepository,
    private val router: NavRouter,
) : ViewModel() {

    var profiles: List<RadarProfile> by mutableStateOf(emptyList())

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
        router.navigate(ScreenNavigationCommands.OpenProfileScreen(null))
    }

    fun onProfileClick(profile: RadarProfile) {
        router.navigate(ScreenNavigationCommands.OpenProfileScreen(profile.id))
    }
}