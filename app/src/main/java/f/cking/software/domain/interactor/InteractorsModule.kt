package f.cking.software.domain.interactor

import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import org.koin.dsl.module

object InteractorsModule {

    val module = module {
        single { FilterCheckerImpl(get(), get(), get(), get(), get()) }
        single { DeviceServicesFetchingPlanner(get(), get(), get()) }

        factory { ClearGarbageInteractor(get(), get(), get(), get()) }
        factory { GetAllDevicesInteractor(get()) }
        factory { IsKnownDeviceInteractor() }
        factory { GetBleRecordFramesFromRawInteractor() }
        factory { GetManufacturerInfoFromRawBleInteractor(get(), get()) }
        factory { BuildDeviceFromScanDataInteractor(get()) }
        factory { GetAirdropInfoFromBleFrame() }
        factory { CheckDeviceIsFollowingInteractor(get()) }
        factory { SaveReportInteractor(get()) }
        factory { BackupDatabaseInteractor(get(), get()) }
        factory { CreateBackupFileInteractor(get(), get()) }
        factory { SelectBackupFileInteractor(get(), get()) }
        factory { RestoreDatabaseInteractor(get(), get()) }
        factory { SaveRadarProfile(get(), get()) }
        factory { DeleteRadarProfile(get(), get()) }
        factory { CheckDeviceLocationHistoryInteractor(get()) }
        factory { CheckUserLocationHistoryInteractor(get()) }
        factory { AddTagToDeviceInteractor(get(), get()) }
        factory { RemoveTagFromDeviceInteractor(get()) }
        factory { ChangeFavoriteInteractor(get()) }
        factory { GetAppUsageDaysInteractor(get()) }
        factory { SaveFirstAppLaunchTimeInteractor(get()) }
        factory { CheckNeedToShowEnjoyTheAppInteractor(get(), get()) }
        factory { EnjoyTheAppAskLaterInteractor(get()) }
        factory { CheckBatchForRadarMatchesInteractor(get(), get(), get(), get(), get()) }
        factory { SaveOrMergeBatchInteractor(get(), get(), get(), get(), get(), get(), get()) }
        factory { FetchDeviceServiceInfo(get(), get()) }
        factory { GetDatabaseInfoInteractor(get(), get()) }
    }
}