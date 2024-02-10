package f.cking.software.ui.filter

import androidx.annotation.StringRes
import f.cking.software.R

enum class FilterType(@StringRes val displayNameRes: Int, @StringRes val displayDescription: Int) {
    BY_LOGIC_ANY(R.string.filter_any_of, R.string.filter_any_of_description),
    BY_LOGIC_ALL(R.string.filter_all_of, R.string.filter_all_of_description),
    BY_LOGIC_NOT(R.string.filter_not, R.string.filter_not_description),
    NAME(R.string.filter_by_name, R.string.filter_by_name_description),
    ADDRESS(R.string.filter_by_address, R.string.filter_by_address_description),
    BY_TAG(R.string.filter_by_tag, R.string.filter_by_tag_description),
    BY_MIN_DETECTION_TIME(R.string.filter_by_min_lost_period, R.string.filter_by_min_lost_period_description),
    BY_FIRST_DETECTION(R.string.filter_by_first_detection_period, R.string.filter_by_first_detection_period_description),
    BY_LAST_DETECTION(R.string.filter_by_last_detection_period, R.string.filter_by_last_detection_period_description),
    BY_IS_FAVORITE(R.string.filter_by_is_favorite, R.string.filter_by_is_favorite_description),
    BY_MANUFACTURER(R.string.filter_by_manufacturer, R.string.filter_by_manufacturer_description),
    IS_FOLLOWING(R.string.filter_device_is_following_me, R.string.filter_device_is_following_me_description),
    BY_DEVICE_LOCATION(R.string.filter_device_location, R.string.filter_device_location_description),
    BY_USER_LOCATION(R.string.filter_user_location, R.string.filter_user_location_description),
    AIRDROP_CONTACT(R.string.filter_apple_airdrop_contact, R.string.filter_apple_airdrop_contact_description),
}