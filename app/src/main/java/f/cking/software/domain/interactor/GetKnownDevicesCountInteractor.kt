package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetKnownDevicesCountInteractor(
    private val devicesRepository: DevicesRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    suspend fun execute(batchAddresses: List<String>): Int {
        return withContext(Dispatchers.Default) {
            devicesRepository.getAllByAddresses(batchAddresses)
                .count(isKnownDeviceInteractor::execute)
        }
    }
}