package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData

class GetAllDevicesInteractor(
    private val devicesRepository: DevicesRepository,
) {

    suspend fun execute(withAirdropInfo: Boolean = false): List<DeviceData> {
        return devicesRepository.getDevices(withAirdropInfo)
    }
}