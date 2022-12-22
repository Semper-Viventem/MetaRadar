package f.cking.software.domain.interactor.filterchecker

import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

interface FilterChecker<T : RadarProfile.Filter> {

    suspend fun check(deviceData: DeviceData, filter: T): Boolean

}