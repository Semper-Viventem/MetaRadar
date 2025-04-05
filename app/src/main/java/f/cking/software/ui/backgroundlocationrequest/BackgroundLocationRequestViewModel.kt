package f.cking.software.ui.backgroundlocationrequest

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.collectAsState
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.flow.map

class BackgroundLocationRequestViewModel(
    private val permissionHelper: PermissionHelper,
    private val router: Router,
) : ViewModel() {
    val grantButtonEnabled by permissionHelper
        .observeBackgroundLocationPermission()
        .map { !it }
        .collectAsState(viewModelScope, true)

    fun onBack() {
        router.navigate(BackCommand)
    }

    fun grantPermission() {
        permissionHelper.checkOrRequestPermission {
            permissionHelper.checkOrRequestPermission(permissions = PermissionHelper.BACKGROUND_LOCATION) {
                onBack()
            }
        }
    }
}
