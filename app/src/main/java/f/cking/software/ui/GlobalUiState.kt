package f.cking.software.ui

import androidx.compose.runtime.mutableStateOf

object GlobalUiState {

    val navbarOffsetPx = mutableStateOf(0f)
    val fabOffsetPx = mutableStateOf(0f)
    val totalOffset = mutableStateOf(navbarOffsetPx.value + fabOffsetPx.value)

    fun setBottomOffset(navbarOffset: Float = navbarOffsetPx.value, fabOffset: Float = fabOffsetPx.value) {
        var updateTotal = false
        if (navbarOffset != navbarOffsetPx.value) {
            navbarOffsetPx.value = navbarOffset
            updateTotal = true
        }
        if (fabOffset != fabOffsetPx.value) {
            fabOffsetPx.value = fabOffset
            updateTotal = true
        }
        if (updateTotal) {
            totalOffset.value = navbarOffset + fabOffset
        }
    }
}