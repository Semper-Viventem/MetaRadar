package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.TagsRepository
import f.cking.software.domain.model.DeviceData

class AddTagToDeviceInteractor(
    private val devicesRepository: DevicesRepository,
    private val tagsRepository: TagsRepository,
) {

    suspend fun execute(device: DeviceData, tag: String) {
        val deviceWithTags = device.copy(tags = device.tags + tag)
        devicesRepository.saveDevice(deviceWithTags)
        tagsRepository.addNewTag(tag)
    }
}