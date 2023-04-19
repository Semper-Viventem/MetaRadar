package f.cking.software.ui.filter

import androidx.annotation.StringRes
import f.cking.software.R

enum class FilterType(@StringRes val displayNameRes: Int) {
    NAME(R.string.filter_by_name),
    ADDRESS(R.string.filter_by_address),
    BY_FIRST_DETECTION(R.string.filter_by_first_detection_period),
    BY_LAST_DETECTION(R.string.filter_by_last_detection_period),
    BY_IS_FAVORITE(R.string.filter_by_is_favorite),
    BY_MANUFACTURER(R.string.filter_by_manufacturer),
    BY_MIN_DETECTION_TIME(R.string.filter_by_min_lost_period),
    AIRDROP_CONTACT(R.string.filter_apple_airdrop_contact),
    IS_FOLLOWING(R.string.filter_device_is_following_me),
    BY_DEVICE_LOCATION(R.string.filter_device_location),
    BY_USER_LOCATION(R.string.filter_user_location),
    BY_LOGIC_ANY(R.string.filter_any_of),
    BY_LOGIC_ALL(R.string.filter_all_of),
    BY_LOGIC_NOT(R.string.filter_not),
}