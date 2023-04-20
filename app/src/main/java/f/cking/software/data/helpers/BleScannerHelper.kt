package f.cking.software.data.helpers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import f.cking.software.domain.interactor.GetKnownDevicesInteractor
import f.cking.software.domain.model.BleScanDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class BleScannerHelper(
    private val getKnownDevicesInteractor: GetKnownDevicesInteractor,
    private val appContext: Context,
    private val powerModeHelper: PowerModeHelper,
) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val batch = hashMapOf<String, BleScanDevice>()
    private var currentScanTimeMs: Long = System.currentTimeMillis()
    private val bleFiltersProvider = BleFiltersProvider(getKnownDevicesInteractor)

    var inProgress = MutableStateFlow(false)

    private var scanListener: ScanListener? = null

    init {
        tryToInitBluetoothScanner()
    }

    private val callback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.scanRecord?.serviceUuids?.map { bleFiltersProvider.previouslyNoticedServicesUUIDs.add(it.uuid.toString()) }
            val device = BleScanDevice(
                address = result.device.address,
                name = result.device.name,
                scanTimeMs = currentScanTimeMs,
                scanRecordRaw = result.scanRecord?.bytes,
            )

            batch.put(device.address, device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed with error: $errorCode")
            cancelScanning(ScanResultInternal.Failure(errorCode))
        }
    }

    fun isBluetoothEnabled(): Boolean {
        tryToInitBluetoothScanner()
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    suspend fun scan(
        scanListener: ScanListener,
    ) {
        Timber.d("Start BLE Scan. Restricted mode: ${powerModeHelper.powerMode().useRestrictedBleConfig}")

        if (!isBluetoothEnabled()) {
            throw BluetoothIsNotInitialized()
        }

        if (inProgress.value) {
            Timber.e("BLE Scan failed because previous scan is not finished")
        } else {
            this@BleScannerHelper.scanListener = scanListener
            batch.clear()

            inProgress.tryEmit(true)
            currentScanTimeMs = System.currentTimeMillis()

            val scanFilters = if (powerModeHelper.powerMode().useRestrictedBleConfig) {
                bleFiltersProvider.getBGFilters() +
                        bleFiltersProvider.getPopularServiceUUIDS() +
                        bleFiltersProvider.getManufacturerFilter()
            } else {
                listOf(ScanFilter.Builder().build())
            }

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            withContext(Dispatchers.IO) {
                requireScanner().startScan(scanFilters, scanSettings, callback)
                handler.postDelayed({ cancelScanning(ScanResultInternal.Success) }, powerModeHelper.powerMode().scanDuration)
            }
        }
    }

    fun stopScanning() {
        cancelScanning(ScanResultInternal.Canceled)
    }

    @SuppressLint("MissingPermission")
    private fun cancelScanning(scanResult: ScanResultInternal) {
        inProgress.tryEmit(false)

        if (bluetoothAdapter?.state == BluetoothAdapter.STATE_ON) {
            bluetoothScanner?.stopScan(callback)
        }

        when (scanResult) {
            is ScanResultInternal.Success -> {
                Timber.d("BLE Scan finished ${batch.count()} devices found")
                scanListener?.onSuccess(batch.values.toList())
            }
            is ScanResultInternal.Failure -> {
                scanListener?.onFailure(BLEScanFailure(scanResult.errorCode))
            }
            is ScanResultInternal.Canceled -> {
                // do nothing
            }
        }
        scanListener = null
    }

    private fun tryToInitBluetoothScanner() {
        bluetoothAdapter = appContext.getSystemService(BluetoothManager::class.java).adapter
        bluetoothScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    private fun requireScanner(): BluetoothLeScanner {
        if (bluetoothScanner == null) {
            tryToInitBluetoothScanner()
        }
        return bluetoothScanner ?: throw BluetoothIsNotInitialized()
    }

    interface ScanListener {
        fun onSuccess(batch: List<BleScanDevice>)
        fun onFailure(exception: Exception)
    }

    private sealed interface ScanResultInternal {

        object Success : ScanResultInternal

        data class Failure(val errorCode: Int) : ScanResultInternal

        object Canceled : ScanResultInternal
    }

    class BLEScanFailure(errorCode: Int) : RuntimeException("BLE Scan failed with error code: $errorCode")

    class BluetoothIsNotInitialized : RuntimeException("Bluetooth is turned off or not available on this device")
}