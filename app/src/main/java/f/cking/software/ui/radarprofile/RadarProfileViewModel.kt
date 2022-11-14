package f.cking.software.ui.radarprofile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.RadarProfile

class RadarProfileViewModel(
    private val radarProfilesRepository: RadarProfilesRepository,
) : ViewModel() {

    val profiles: List<RadarProfile> by mutableStateOf(emptyList())

    fun createNewClick() {

    }

    fun onProfileClick(profile: RadarProfile) {

    }
}