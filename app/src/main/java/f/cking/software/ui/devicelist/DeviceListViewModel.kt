package f.cking.software.ui.devicelist

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.checkRegexSafe
import f.cking.software.collectAsState
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.CheckNeedToShowEnjoyTheAppInteractor
import f.cking.software.domain.interactor.EnjoyTheAppAskLaterInteractor
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.DeviceClass
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.mapParallel
import f.cking.software.service.BgScanService
import f.cking.software.splitToBatches
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.utils.navigation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.ext.getFullName
import java.util.concurrent.TimeUnit

class DeviceListViewModel(
    private val context: Application,
    private val devicesRepository: DevicesRepository,
    private val filterCheckerImpl: FilterCheckerImpl,
    permissionHelper: PermissionHelper,
    val router: Router,
    private val checkNeedToShowEnjoyTheAppInteractor: CheckNeedToShowEnjoyTheAppInteractor,
    private val enjoyTheAppAskLaterInteractor: EnjoyTheAppAskLaterInteractor,
    private val settingsRepository: SettingsRepository,
    private val intentHelper: IntentHelper,
) : ViewModel() {

    var currentBatchSortingStrategy by mutableStateOf(getDefaultSortStrategy())
    var devicesViewState by mutableStateOf(emptyList<DeviceData>())
    var activeScannerExpandedState by mutableStateOf(ActiveScannerExpandedState.COLLAPSED)
    var currentBatchViewState by mutableStateOf<List<DeviceData>?>(null)
    var appliedFilter: MutableStateFlow<List<FilterHolder>> = MutableStateFlow(emptyList())
    var searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)
    var isSearchMode: Boolean by mutableStateOf(false)
    var isLoading: Boolean by mutableStateOf(false)
    var isPaginationEnabled: Boolean by mutableStateOf(false)
    var quickFilters: List<FilterHolder> by mutableStateOf(
        listOf(
            DefaultFilters.notApple(context),
            DefaultFilters.isFavorite(context),
        )
    )
    var enjoyTheAppState: EnjoyTheAppState by mutableStateOf(EnjoyTheAppState.None)
    val showBackgroundPermissionWarning: Boolean by combine(
        permissionHelper.observeBackgroundLocationPermission(),
        settingsRepository.observeHideBackgroundLocationWarning(),
    ) { permissionGranted, hideWarningTime -> !permissionGranted && checkBackgroundWarningIsExpired(hideWarningTime) }
        .collectAsState(viewModelScope, false)
    val areFiltersApplied by combine(appliedFilter, searchQuery) { filters, query -> filters.isNotEmpty() || !query.isNullOrBlank() }
        .collectAsState(viewModelScope, false)

    private var scannerObservingJob: Job? = null
    private var lastBatchJob: Job? = null
    private var currentPage: Int by mutableStateOf(INITIAL_PAGE)

    init {
        observeIsScannerEnabled()
    }

    fun onFilterClick(filter: FilterHolder) {
        val newFilters = appliedFilter.value.toMutableList()
        if (newFilters.contains(filter)) {
            newFilters.remove(filter)
        } else {
            newFilters.add(filter)
        }
        viewModelScope.launch { appliedFilter.emit(newFilters) }
    }

    fun onOpenSearchClick() {
        isSearchMode = !isSearchMode
        if (!isSearchMode) {
            viewModelScope.launch { searchQuery.emit(null) }
        }
    }

    fun onSearchInput(str: String) {
        viewModelScope.launch { searchQuery.emit(str) }
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

    private fun observeIsScannerEnabled() {
        viewModelScope.launch {
            BgScanService.observeIsActive()
                .collect { checkScreenMode(true) }
        }
    }

    private fun checkScreenMode(invalidateCurrentBatch: Boolean) {
        val isScannerEnabled = BgScanService.isActive
        val anyFilterApplyed = isSearchMode || appliedFilter.value.isNotEmpty()

        scannerObservingJob?.cancel()
        disablePagination()

        // TODO fix realtime items observing before enabling pagination
//        if (isScannerEnabled || anyFilterApplyed) {
//            disablePagination()
//        } else {
//            enablePagination()
//        }

        if (invalidateCurrentBatch) {
            lastBatchJob?.cancel()
            if (isScannerEnabled) {
                currentBatchViewState = emptyList()
                lastBatchJob = observeCurrentBatch()
            } else {
                currentBatchViewState = null
                lastBatchJob = null
            }
        }
    }

    private fun disablePagination() {
        isPaginationEnabled = false
        scannerObservingJob = observeAllDevices()
    }

    fun onBackgraundLocationWarningClick() {
        router.navigate(ScreenNavigationCommands.OpenBackgroundLocationScreen)
    }

    fun onHideBackgroundLocationWarningClick() {
        settingsRepository.setHideBackgroundLocationWarning(System.currentTimeMillis())
    }

    private fun checkBackgroundWarningIsExpired(hideMessageTime: Long): Boolean {
        return System.currentTimeMillis() - hideMessageTime > TimeUnit.DAYS.toMillis(3)
    }

    fun applyCurrentBatchSortingStrategy(strategy: CurrentBatchSortingStrategy) {
        currentBatchSortingStrategy = strategy
        settingsRepository.setCurrentBatchSortingStrategyId(strategy.ordinal)
        currentBatchViewState = currentBatchViewState?.sortedWith(strategy.comparator)
    }

    @ExperimentalCoroutinesApi
    private fun observeCurrentBatch(): Job {
        return viewModelScope.launch {
            combine(
                appliedFilter,
                searchQuery,
                devicesRepository.observeLastBatch()
                    .onStart {
                        currentBatchViewState = emptyList()
                        devicesRepository.clearLastBatch()
                    }
            ) { filters, query, devices ->
                val devices = devices
                    .withFilters(filters, query)
                    .sortedWith(currentBatchSortingStrategy.comparator)
                devices
            }
                .collect { devices ->
                    currentBatchViewState = devices
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAllDevices(): Job {
        isLoading = true
        return viewModelScope.launch {
            combine(
                appliedFilter,
                searchQuery,
                devicesRepository.observeAllDevices(),
            ) { filters, query, devices -> Triple(filters, query, devices) }
                .flatMapLatest { (filters, query, devices) ->
                    flow {
                        val result = withContext(Dispatchers.Default) {
                            isLoading = true
                            devices
                                .withFilters(filters, query)
                                .sortedWith(GENERAL_COMPARATOR)
                                .apply { showEnjoyTheAppIfNeeded() }
                        }
                        emit(result)
                    }
                }
                .onStart {
                    isLoading = true
                }
                .collect { devices ->
                    isLoading = false
                    devicesViewState = devices
                }
        }
    }

    private suspend inline fun List<DeviceData>.withFilters(
        appliedFilters: List<FilterHolder>,
        searchQuery: String?,
    ): List<DeviceData> {
        val filter = when {
            appliedFilters.isEmpty() -> null
            appliedFilters.size == 1 -> appliedFilters.first().filter
            else -> RadarProfile.Filter.All(appliedFilters.map { it.filter })
        }
        val query = searchQuery

        return if (filter == null && query == null) {
            this
        } else {
            this.splitToBatches(100)
                .mapParallel { batch ->
                    batch.filter { checkFilter(it, filter) && filterQuery(it, query) }
                }
                .flatMap { it }
        }
    }

    private suspend fun showEnjoyTheAppIfNeeded() {
        if (enjoyTheAppState is EnjoyTheAppState.None && checkNeedToShowEnjoyTheAppInteractor.execute()) {
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

    private fun filterQuery(device: DeviceData, query: String?): Boolean {
        return query?.takeIf { it.isNotBlank() }?.let { searchStr ->
            (device.resolvedName?.contains(searchStr, true) == true)
                    || (device.metadata?.deviceName?.contains(searchStr, true) == true)
                    || (device.metadata?.manufacturerName?.contains(searchStr, true) == true)
                    || (device.metadata?.modelNumber?.contains(searchStr, true) == true)
                    || (device.customName?.contains(searchStr, true) == true)
                    || (device.manufacturerInfo?.name?.contains(searchStr, true) == true)
                    || device.address.contains(searchStr, true)
                    || device.address.checkRegexSafe(query)
                    || (device.resolvedName?.checkRegexSafe(query) == true)
        } != false
    }

    private suspend fun checkFilter(device: DeviceData, filter: RadarProfile.Filter?): Boolean {
        return if (filter != null) {
            filterCheckerImpl.check(device, filter)
        } else {
            true
        }
    }

    private fun getDefaultSortStrategy(): CurrentBatchSortingStrategy {
        val id = settingsRepository.getCurrentBatchSortingStrategyId()
        return CurrentBatchSortingStrategy.entries.getOrElse(id) { CurrentBatchSortingStrategy.GENERAL }
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

    enum class CurrentBatchSortingStrategy(
        val comparator: Comparator<DeviceData>,
        @StringRes val displayNameRes: Int,
    ) {
        GENERAL(GENERAL_COMPARATOR, R.string.sort_type_standart),
        BY_DISTANCE(Comparator { second, first ->
            when {
                first.distance() != second.distance() -> second.distance()?.compareTo(first.distance() ?: return@Comparator 1) ?: -1
                else -> GENERAL_COMPARATOR.compare(first, second)
            }
        }, R.string.sort_type_by_distance),
        BY_TYPE(Comparator { first, second ->
            val firstType = first.resolvedDeviceClass
            val secondType = second.resolvedDeviceClass
            when {
                firstType !is DeviceClass.Unknown && secondType is DeviceClass.Unknown -> -1
                firstType is DeviceClass.Unknown && secondType !is DeviceClass.Unknown -> 1
                firstType != secondType -> firstType::class.getFullName().compareTo(secondType::class.getFullName())
                else -> GENERAL_COMPARATOR.compare(first, second)
            }
        }, R.string.sort_type_by_device_type)
    }

    enum class ActiveScannerExpandedState {
        EXPANDED, COLLAPSED;

        fun next(): ActiveScannerExpandedState {
            return entries.elementAt((ordinal + 1) % entries.size)
        }

        companion object {
            val MAX_DEVICES_COUNT = 3
        }
    }

    companion object {
        private const val PAGE_SIZE = 40
        private const val INITIAL_PAGE = 0

        private val GENERAL_COMPARATOR = Comparator<DeviceData> { second, first ->

            when {
                first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)

                first.tags.size != second.tags.size -> first.tags.size.compareTo(second.tags.size)
                first.favorite && !second.favorite -> 1
                !first.favorite && second.favorite -> -1

                first.resolvedName != second.resolvedName -> first.resolvedName?.compareTo(second.resolvedName ?: return@Comparator 1) ?: -1

                first.rssi != second.rssi -> first.rssi?.compareTo(second.rssi ?: return@Comparator 1) ?: -1

                first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                    first.manufacturerInfo?.name?.compareTo(second.manufacturerInfo?.name ?: return@Comparator 1) ?: -1

                else -> first.address.compareTo(second.address)
            }
        }
    }
}