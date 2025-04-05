package f.cking.software.domain.interactor

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import f.cking.software.data.helpers.BleScannerHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.DeviceMetadata
import f.cking.software.domain.model.DeviceMetadata.CharacteristicType
import f.cking.software.domain.model.DeviceMetadata.ServiceTypes
import f.cking.software.domain.model.isNullOrEmpty
import f.cking.software.fromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class FetchDeviceServiceInfo(
    private val bleScannerHelper: BleScannerHelper,
    private val devicesRepository: DevicesRepository,
) {
    suspend fun execute(device: DeviceData): DeviceMetadata? =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            Timber.tag(TAG).i("Fetching device info for ${device.address}")
            val originalMetadata = device.metadata
            val updatedMetadata = connectAndFetchServices(device).firstOrNull()
            if (originalMetadata != updatedMetadata) {
                devicesRepository.saveDevice(device.copy(metadata = updatedMetadata))
            }
            val duration = System.currentTimeMillis() - start
            Timber.tag(TAG).i("Fetching is finished after $duration ms for ${device.address}, metadata: $updatedMetadata")
            updatedMetadata
        }

    @OptIn(FlowPreview::class)
    private suspend fun connectAndFetchServices(device: DeviceData): Flow<DeviceMetadata> =
        coroutineScope {
            flow<DeviceMetadata> {
                val pendingCharacteristics = mutableMapOf<String, BluetoothGattCharacteristic>()
                var metadata = device.metadata ?: DeviceMetadata()
                var gatt: BluetoothGatt? = null
                var job: Job? = null

                Timber.tag(TAG).i("Connecting to ${device.address}")

                suspend fun submitMetadata() {
                    Timber.tag(TAG).i("Closing connection ${device.address}")
                    gatt?.let(bleScannerHelper::close)
                    gatt = null // to be sure
                    job?.cancel()
                    emit(metadata)
                }

                fun disconnect(gatt: BluetoothGatt) {
                    Timber.tag(TAG).i("Disconnecting from ${device.address}")
                    bleScannerHelper.disconnect(gatt)

                    job =
                        this@coroutineScope.async {
                            delay(100)
                            Timber.tag(TAG).i("Disconnecting from ${device.address} takes too long, closing connection")
                            submitMetadata()
                        }
                }

                fun throwIfMetadataNotUpdated(e: Exception) {
                    if (metadata.isNullOrEmpty() || metadata == device.metadata) {
                        throw e
                    }
                }

                bleScannerHelper
                    .connectToDevice(device.address)
                    .collect { event ->
                        when (event) {
                            is BleScannerHelper.DeviceConnectResult.Connected -> {
                                Timber.tag(TAG).i("Connected to ${device.address}. Discovering services...")
                                gatt = event.gatt
                                bleScannerHelper.discoverServices(event.gatt)
                            }

                            is BleScannerHelper.DeviceConnectResult.AvailableServices -> {
                                if (pendingCharacteristics.isEmpty()) {
                                    val relevantCharacteristics = findRelevantCharacteristics(event.services)
                                    Timber
                                        .tag(TAG)
                                        .i(
                                            "Services discovered for ${device.address}. Relevant characteristics: ${
                                                relevantCharacteristics.map {
                                                    it.uuid
                                                        .toString()
                                                }
                                            }",
                                        )
                                    if (relevantCharacteristics.isNotEmpty()) {
                                        pendingCharacteristics.putAll(relevantCharacteristics.associateBy { it.uuid.toString() })
                                        requestCharacteristic(event.gatt, relevantCharacteristics.first())
                                    } else {
                                        Timber.tag(TAG).i("No relevant characteristics found for ${device.address}")
                                        disconnect(event.gatt)
                                    }
                                } else {
                                    Timber.tag(TAG).i("No services to request for ${device.address}")
                                    disconnect(event.gatt)
                                }
                            }

                            is BleScannerHelper.DeviceConnectResult.CharacteristicRead -> {
                                val characteristic = event.characteristic
                                val value = event.valueEncoded64.fromBase64()
                                val uuid = characteristic.uuid.toString()
                                Timber
                                    .tag(
                                        TAG,
                                    ).i("Characteristic read for ${device.address}: Characteristic data $uuid: ${value.decodeToString()}")

                                metadata =
                                    when (CharacteristicType.findByUuid(uuid)) {
                                        CharacteristicType.DEVICE_NAME -> {
                                            metadata.copy(deviceName = value.decodeToString())
                                        }

                                        CharacteristicType.MANUFACTURER_NAME -> {
                                            metadata.copy(manufacturerName = value.decodeToString())
                                        }

                                        CharacteristicType.MODEL_NUMBER -> {
                                            metadata.copy(modelNumber = value.decodeToString())
                                        }

                                        CharacteristicType.SERIAL_NUMBER -> {
                                            metadata.copy(serialNumber = value.decodeToString())
                                        }

                                        CharacteristicType.BATTERY_LEVEL -> {
                                            metadata.copy(batteryLevel = value.getOrNull(0)?.toInt())
                                        }

                                        else -> metadata
                                    }
                                pendingCharacteristics.remove(uuid)

                                if (pendingCharacteristics.isEmpty()) {
                                    Timber.tag(TAG).i("All characteristics read for ${device.address}, finishing fetching...")
                                    disconnect(event.gatt)
                                } else {
                                    Timber.tag(TAG).i("Still pending characteristics for ${device.address}: ${pendingCharacteristics.keys}")
                                    requestCharacteristic(event.gatt, pendingCharacteristics.values.first())
                                }
                            }

                            is BleScannerHelper.DeviceConnectResult.FailedReadCharacteristic -> {
                                val uuid = event.characteristic.uuid.toString()
                                Timber.tag(TAG).e("Failed to read characteristic ${event.characteristic.uuid} for ${device.address}")
                                pendingCharacteristics.remove(uuid)

                                if (pendingCharacteristics.isEmpty()) {
                                    Timber.tag(TAG).i("All characteristics read for ${device.address}, finishing fetching...")
                                    disconnect(event.gatt)
                                } else {
                                    Timber.tag(TAG).i("Still pending characteristics for ${device.address}: $pendingCharacteristics.keys")
                                    requestCharacteristic(event.gatt, pendingCharacteristics.values.first())
                                }
                            }

                            is BleScannerHelper.DeviceConnectResult.Disconnected -> {
                                Timber.tag(TAG).i("Disconnected from ${device.address}")
                                submitMetadata()
                            }

                            // Error handling
                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.UnspecifiedConnectionError -> {
                                Timber.tag(TAG).e("Unspecified connection error from ${device.address}.")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.UnspecifiedConnectionError(event.errorCode))
                                submitMetadata()
                            }

                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.ConnectionTimeout -> {
                                Timber.tag(TAG).e("Connection timeout error from ${device.address}")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.ConnectionTimeoutException(event.errorCode))
                                submitMetadata()
                            }

                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.ConnectionFailedToEstablish -> {
                                Timber.tag(TAG).e("Connection failed to establish error from ${device.address}")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.ConnectionFailedToEstablish(event.errorCode))
                                submitMetadata()
                            }

                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.ConnectionFailedBeforeInitializing -> {
                                Timber.tag(TAG).e("Connection initializing failed error from ${device.address}")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.ConnectionInitializingFailed(event.errorCode))
                                submitMetadata()
                            }

                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.ConnectionTerminated -> {
                                Timber
                                    .tag(
                                        TAG,
                                    ).e("Connection terminated error from ${device.address}. Probably max GATT connections reached")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.ConnectionTerminated(event.errorCode))
                                submitMetadata()
                            }

                            is BleScannerHelper.DeviceConnectResult.DisconnectedWithError.ConnectionFailedTooManyClients -> {
                                Timber.tag(TAG).e("Connection failed due to too many clients error from ${device.address}")
                                throwIfMetadataNotUpdated(BluetoothConnectionException.TooManyClients(event.errorCode))
                                submitMetadata()
                            }

                            else -> {
                                // do nothing
                            }
                        }
                    }
            }
        }

    private fun requestCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        Timber.tag(TAG).i("Requesting characteristic ${characteristic.uuid}")
        bleScannerHelper.readCharacteristic(gatt, characteristic)
    }

    private fun findRelevantCharacteristics(services: List<BluetoothGattService>): List<BluetoothGattCharacteristic> =
        services.flatMap { service ->
            val isRelevant = ServiceTypes.findByUuid(service.uuid.toString()) != null
            if (isRelevant) {
                filterRelevantCharacteristics(service.characteristics)
            } else {
                emptyList()
            }
        }

    private fun filterRelevantCharacteristics(characteristics: List<BluetoothGattCharacteristic>): List<BluetoothGattCharacteristic> =
        characteristics.filter {
            CharacteristicType.findByUuid(it.uuid.toString()) != null
        }

    sealed class BluetoothConnectionException(
        message: String,
    ) : RuntimeException(message) {
        class UnspecifiedConnectionError(
            errorStatus: Int,
        ) : BluetoothConnectionException("Unspecified connection error (status: $errorStatus)")

        class ConnectionTimeoutException(
            errorStatus: Int,
        ) : BluetoothConnectionException("Connection timeout (status: $errorStatus)")

        class ConnectionFailedToEstablish(
            errorStatus: Int,
        ) : BluetoothConnectionException("Connection failed to establish (status: $errorStatus)")

        class ConnectionInitializingFailed(
            errorStatus: Int,
        ) : BluetoothConnectionException("Connection initializing failed (code $errorStatus)")

        class ConnectionTerminated(
            errorStatus: Int,
        ) : BluetoothConnectionException("Connection terminated (status: $errorStatus)")

        class TooManyClients(
            errorStatus: Int,
        ) : BluetoothConnectionException("Too many clients (status: $errorStatus)")
    }

    companion object {
        private const val TAG = "FetchDeviceServiceInfo"
    }
}
