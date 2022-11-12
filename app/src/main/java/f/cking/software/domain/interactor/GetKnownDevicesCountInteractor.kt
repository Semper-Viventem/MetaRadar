package f.cking.software.domain.interactor

import f.cking.software.domain.repo.DevicesRepository

class GetKnownDevicesCountInteractor(
    private val devicesRepository: DevicesRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    fun execute(batchAddresses: List<String>): Int {
        return devicesRepository.getAllByAddresses(batchAddresses).count(isKnownDeviceInteractor::execute)
    }
}