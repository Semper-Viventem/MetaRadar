package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData

class ChangeFavoriteInteractor(
    private val devicesRepository: DevicesRepository,
) {

    suspend fun execute(device: DeviceData) {
        val new = device.copy(favorite = !device.favorite)
        devicesRepository.saveDevice(new)
    }
}