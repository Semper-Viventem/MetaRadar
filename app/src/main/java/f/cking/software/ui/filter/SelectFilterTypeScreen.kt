package f.cking.software.ui.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import f.cking.software.R

object SelectFilterTypeScreen {

    @Composable
    fun Dialog(
        dialogState: MaterialDialogState,
        onSelected: (type: FilterUiState) -> Unit,
    ) {

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(stringResource(R.string.cancel)) { dialogState.hide() }
            }
        ) {
            LazyColumn {
                FilterType.values().forEach { type ->
                    item {
                        TypeItem(item = type) {
                            dialogState.hide()
                            onSelected.invoke(getFilterByType(type))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TypeItem(item: FilterType, onClickListener: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClickListener.invoke() }
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(item.displayNameRes),
                fontSize = 18.sp
            )
        }
    }

    private fun getFilterByType(type: FilterType): FilterUiState {
        return when (type) {
            FilterType.NAME -> FilterUiState.Name()
            FilterType.ADDRESS -> FilterUiState.Address()
            FilterType.BY_LAST_DETECTION -> FilterUiState.LastDetectionInterval()
            FilterType.BY_FIRST_DETECTION -> FilterUiState.FirstDetectionInterval()
            FilterType.BY_IS_FAVORITE -> FilterUiState.IsFavorite()
            FilterType.BY_MANUFACTURER -> FilterUiState.Manufacturer()
            FilterType.BY_LOGIC_ALL -> FilterUiState.All()
            FilterType.BY_LOGIC_ANY -> FilterUiState.Any()
            FilterType.BY_LOGIC_NOT -> FilterUiState.Not()
            FilterType.BY_MIN_DETECTION_TIME -> FilterUiState.MinLostTime()
            FilterType.AIRDROP_CONTACT -> FilterUiState.AppleAirdropContact()
            FilterType.IS_FOLLOWING -> FilterUiState.IsFollowing()
            FilterType.BY_DEVICE_LOCATION -> FilterUiState.DeviceLocation()
        }
    }
}