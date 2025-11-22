package f.cking.software.data.helpers

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import f.cking.software.domain.interactor.GetAllDevicesInteractor
import f.cking.software.domain.model.ManufacturerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BleFiltersProvider(
    private val getAllDevicesInteractor: GetAllDevicesInteractor,
) {

    val previouslyNoticedServicesUUIDs = mutableSetOf<String>()

    suspend fun getBackgroundFilters(): List<ScanFilter> {
        return getKnownDevicesFilters() + getPopularServiceUUIDS() + getManufacturerFilter()
    }

    fun getPopularServiceUUIDS(): List<ScanFilter> {
        return (popularServicesUUID + previouslyNoticedServicesUUIDs).map { uuid ->
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(uuid))
                .build()
        }
    }

    fun getManufacturerFilter(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder()
                .setManufacturerData(
                    ManufacturerInfo.APPLE_ID,
                    NearByData.bytes,
                    NearByData.bytesMask,
                )
                .build(),
            ScanFilter.Builder()
                .setManufacturerData(
                    ManufacturerInfo.APPLE_ID,
                    AirdropData.bytes,
                    AirdropData.bytesMask,
                )
                .build(),
        )

    }

    suspend fun getKnownDevicesFilters(): List<ScanFilter> {
        return withContext(Dispatchers.Default) {
            val allKnownDevices = getAllDevicesInteractor.execute(withAirdropInfo = false)

            val lastSeenDevices = allKnownDevices
                .sortedByDescending { it.lastDetectTimeMs }
                .take(KNOWN_DEVICES_LIMIT) // Limit filters to fit into android scan registerer limitations
                .map { it.address }
                .toSet()

            val shouldBeIncluded = allKnownDevices
                .filter { it.tags.isNotEmpty() || it.favorite }
                .map { it.address }
                .toSet()

            (lastSeenDevices + shouldBeIncluded)
                .map {
                    ScanFilter.Builder()
                        .setDeviceAddress(it)
                        .build()
                }
        }
    }

    private object NearByData {
        val bytes: ByteArray = listOf(0x10, 0x07, 0x38, 0x1F, 0x0E, 0x67, 0x06, 0x71, 0x18)
            .map { it.toUByte().toByte() }
            .toByteArray()

        val bytesMask: ByteArray = listOf(0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            .map { it.toUByte().toByte() }
            .toByteArray()
    }

    private object AirdropData {
        val bytes: ByteArray =
            listOf(0x05, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x0B, 0x0D, 0xC1, 0xF6, 0xFA, 0xE3, 0x11, 0x00, 0x00)
                .map { it.toUByte().toByte() }
                .toByteArray()
        val bytesMask: ByteArray =
            listOf(0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                .map { it.toUByte().toByte() }
                .toByteArray()
    }

    companion object {
        private const val KNOWN_DEVICES_LIMIT = 3000
        private val popularServicesUUID = setOf(
            "0000fe8f-0000-1000-8000-00805f9b34fb",
            "0000fe9f-0000-1000-8000-00805f9b34fb",
            "0000feb8-0000-1000-8000-00805f9b34fb",
            "0000fd5a-0000-1000-8000-00805f9b34fb",
        )
    }
}