package f.cking.software.ui.devicelist

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.CheckNeedToShowEnjoyTheAppInteractor
import f.cking.software.domain.interactor.EnjoyTheAppAskLaterInteractor
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceListViewModel(
    private val context: Application,
    private val devicesRepository: DevicesRepository,
    private val filterCheckerImpl: FilterCheckerImpl,
    val router: Router,
    private val checkNeedToShowEnjoyTheAppInteractor: CheckNeedToShowEnjoyTheAppInteractor,
    private val enjoyTheAppAskLaterInteractor: EnjoyTheAppAskLaterInteractor,
    private val settingsRepository: SettingsRepository,
    private val intentHelper: IntentHelper,
) : ViewModel() {

    var devicesViewState by mutableStateOf(emptyList<DeviceData>())
    var appliedFilter: List<FilterHolder> by mutableStateOf(emptyList())
    var searchQuery: String? by mutableStateOf(null)
    var isSearchMode: Boolean by mutableStateOf(false)
    var isLoading: Boolean by mutableStateOf(false)
    var quickFilters: List<FilterHolder> by mutableStateOf(
        listOf(
            DefaultFilters.notApple(context),
            DefaultFilters.isFavorite(context),
        )
    )
    var enjoyTheAppState: EnjoyTheAppState by mutableStateOf(EnjoyTheAppState.None)

    private val generalComparator = Comparator<DeviceData> { second, first ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)

            first.tags.size != second.tags.size -> first.tags.size.compareTo(second.tags.size)
            first.favorite && !second.favorite -> 1
            !first.favorite && second.favorite -> -1

            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1) ?: -1

            first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                first.manufacturerInfo?.name?.compareTo(second.manufacturerInfo?.name ?: return@Comparator 1) ?: -1

            else -> first.address.compareTo(second.address)
        }
    }

    init {
        observeDevices()
    }

    fun onFilterClick(filter: FilterHolder) {
        val newFilters = appliedFilter.toMutableList()
        if (newFilters.contains(filter)) {
            newFilters.remove(filter)
        } else {
            newFilters.add(filter)
        }
        appliedFilter = newFilters
        fetchDevices()
    }

    fun onOpenSearchClick() {
        isSearchMode = !isSearchMode
        if (!isSearchMode) {
            searchQuery = null
        }
        fetchDevices()
    }

    fun onSearchInput(str: String) {
        searchQuery = str
        fetchDevices()
    }

    fun onDeviceClick(device: DeviceData) {
        router.navigate(ScreenNavigationCommands.OpenDeviceDetailsScreen(device.address))
    }

    fun onTagSelected(tag: String) {
        val tagFilter = FilterHolder(
            displayName = tag,
            filter = RadarProfile.Filter.ByTag(tag),
        )
        onFilterClick(tagFilter)
    }

    private fun observeDevices() {
        viewModelScope.launch {
            isLoading = true
            devicesRepository.observeDevices()
                .collect { devices ->
                    isLoading = true
                    applyDevices(devices)
                    isLoading = false
                }
        }
    }

    private fun fetchDevices() {
        viewModelScope.launch {
            isLoading = true
            val devices = devicesRepository.getDevices()
            applyDevices(devices)
            isLoading = false
        }
    }

    private suspend fun applyDevices(devices: List<DeviceData>) {
        val filter = withContext(Dispatchers.Main) {
            when {
                appliedFilter.isEmpty() -> null
                appliedFilter.size == 1 -> appliedFilter.first().filter
                else -> RadarProfile.Filter.All(appliedFilter.map { it.filter })
            }
        }

        val query = withContext(Dispatchers.Main) {
            searchQuery
        }

        devicesViewState = withContext(Dispatchers.Default) {
            devices
                .filter { checkFilter(it, filter) && filterQuery(it, query) }
                .sortedWith(generalComparator)
                .apply {
                    if (size >= MIN_DEVICES_FOR_ENJOY_THE_APP) {
                        showEnjoyTheAppIfNeeded()
                    }
                }
        }
    }

    private fun showEnjoyTheAppIfNeeded() {
        if (enjoyTheAppState is EnjoyTheAppState.None
            && checkNeedToShowEnjoyTheAppInteractor.execute()
        ) {
            enjoyTheAppState = EnjoyTheAppState.Question
        }
    }

    fun onEnjoyTheAppAnswered(answer: EnjoyTheAppAnswer) {
        enjoyTheAppState = when (answer) {
            EnjoyTheAppAnswer.LIKE -> buildLikeTheAppState()
            EnjoyTheAppAnswer.DISLIKE -> EnjoyTheAppState.Dislike
            EnjoyTheAppAnswer.ASK_LATER -> {
                enjoyTheAppAskLaterInteractor.execute()
                EnjoyTheAppState.None
            }
        }
    }

    private fun buildLikeTheAppState(): EnjoyTheAppState.Like {
        val actions = mutableListOf<EnjoyTheAppState.RateAppAction>()
        if (BuildConfig.STORE_RATING_IS_APPLICABLE) {
            actions.add(
                EnjoyTheAppState.RateAppAction(
                    title = BuildConfig.DISTRIBUTION,
                    url = BuildConfig.STORE_PAGE_URL,
                    saveAnswer = true,
                )
            )
        }
        actions.add(
            EnjoyTheAppState.RateAppAction(
                title = context.getString(R.string.rate_the_app_github),
                url = BuildConfig.GITHUB_URL,
                saveAnswer = !BuildConfig.STORE_RATING_IS_APPLICABLE,
            )
        )
        return EnjoyTheAppState.Like(actions)
    }

    fun onRateButtonClick(rateAction: EnjoyTheAppState.RateAppAction) {
        if (rateAction.saveAnswer) {
            settingsRepository.setEnjoyTheAppAnswered(true)
            enjoyTheAppState = EnjoyTheAppState.None
        }
        intentHelper.openUrl(rateAction.url)
    }

    fun onEnjoyTheAppReportClick() {
        settingsRepository.setEnjoyTheAppAnswered(true)
        enjoyTheAppState = EnjoyTheAppState.None
        intentHelper.openUrl(BuildConfig.REPORT_ISSUE_URL)
    }

    fun filterQuery(device: DeviceData, query: String?): Boolean {
        return query?.takeIf { it.isNotBlank() }?.let { searchStr ->
            (device.name?.contains(searchStr, true) ?: false)
                    || (device.customName?.contains(searchStr, true) ?: false)
                    || (device.manufacturerInfo?.name?.contains(searchStr, true) ?: false)
                    || device.address.contains(searchStr, true)
        } ?: true
    }

    private suspend fun checkFilter(device: DeviceData, filter: RadarProfile.Filter?): Boolean {
        return if (filter != null) {
            filterCheckerImpl.check(device, filter)
        } else {
            true
        }
    }

    data class FilterHolder(
        val displayName: String,
        val filter: RadarProfile.Filter,
    )

    sealed interface EnjoyTheAppState {
        data object None : EnjoyTheAppState
        data object Question : EnjoyTheAppState

        data class Like(
            val actions: List<RateAppAction>
        ) : EnjoyTheAppState

        data object Dislike : EnjoyTheAppState

        data class RateAppAction(
            val title: String,
            val url: String,
            val saveAnswer: Boolean,
        )
    }

    enum class EnjoyTheAppAnswer {
        LIKE, DISLIKE, ASK_LATER
    }

    object DefaultFilters {

        fun notApple(context: Context) = FilterHolder(
            displayName = context.getString(R.string.not_apple),
            filter = RadarProfile.Filter.Not(
                filter = RadarProfile.Filter.Manufacturer(ManufacturerInfo.APPLE_ID)
            )
        )

        fun isFavorite(context: Context) = FilterHolder(
            displayName = context.getString(R.string.favorite),
            filter = RadarProfile.Filter.IsFavorite(favorite = true)
        )
    }

    companion object {
        private const val MIN_DEVICES_FOR_ENJOY_THE_APP = 10
    }
}