package f.cking.software.domain.interactor

import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import org.koin.dsl.module

object InteractorsModule {

    val module = module {
        factory { AnalyseScanBatchInteractor(get(), get()) }
        factory { ClearGarbageInteractor(get(), get(), get(), get()) }
        factory { GetKnownDevicesCountInteractor(get(), get()) }
        factory { GetKnownDevicesInteractor(get(), get()) }
        factory { IsKnownDeviceInteractor() }
        factory { SaveScanBatchInteractor(get(), get(), get(), get()) }
        factory { GetBleRecordFramesFromRawInteractor() }
        factory { GetManufacturerInfoFromRawBleInteractor(get(), get()) }
        factory { CheckProfileDetectionInteractor(get(), get(), get(), get(), get(), get()) }
        factory { BuildDeviceFromScanDataInteractor(get()) }
        factory { GetAirdropInfoFromBleFrame() }
        factory { FilterCheckerImpl(get(), get()) }
        factory { CheckDeviceIsFollowingInteractor(get()) }
        factory { SaveReportInteractor(get()) }
        factory { BackupDatabaseInteractor(get(), get()) }
        factory { CreateBackupFileInteractor(get(), get()) }
        factory { SelectBackupFileInteractor(get(), get()) }
        factory { RestoreDatabaseInteractor(get(), get(), get()) }
    }
}