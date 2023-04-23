package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData

class RemoveTagFromDeviceInteractor(
    private val devicesRepository: DevicesRepository,
) {

    suspend fun execute(device: DeviceData, tag: String) {
        val deviceWithTags = device.copy(tags = device.tags.filter { it != tag }.toSet())
        devicesRepository.saveDevice(deviceWithTags)
    }
}