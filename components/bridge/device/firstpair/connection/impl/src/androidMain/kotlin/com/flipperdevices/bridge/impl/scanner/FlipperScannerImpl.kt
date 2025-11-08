package com.flipperdevices.bridge.impl.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.flipperdevices.bridge.api.scanner.DiscoveredBluetoothDevice
import com.flipperdevices.bridge.api.scanner.FlipperScanner
import com.flipperdevices.bridge.api.utils.Constants
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.info
import dev.zacsweers.metro.ContributesBinding
import me.tatarka.inject.annotations.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.distinctByPeripheral
import kotlin.collections.map
import kotlin.uuid.toKotlinUuid

@Inject
@ContributesBinding(BusyLibGraph::class, FlipperScanner::class)
class FlipperScannerImpl(
    private val centralManager: CentralManager,
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : FlipperScanner, LogTagProvider {
    override val TAG = "FlipperScanner"

    override fun findFlipperDevices(): Flow<Iterable<DiscoveredBluetoothDevice>> {
        val devices = mutableListOf<DiscoveredBluetoothDeviceImpl>()
        val mutex = Mutex()

        return merge(
            getAlreadyBondedDevices(),
            getScanDevices()
        ).map { discoveredBluetoothDevice ->
            var mutableDevicesList: List<DiscoveredBluetoothDevice> = emptyList()
            mutex.withLock {
                val alreadyExistDBD = devices.getOrNull(
                    devices.indexOf(discoveredBluetoothDevice)
                )
                if (alreadyExistDBD != null) {
                    val scanResult = discoveredBluetoothDevice.scanResult
                    if (scanResult != null) {
                        alreadyExistDBD.update(scanResult)
                    }
                } else {
                    info { "Find new device $discoveredBluetoothDevice" }
                    devices.add(discoveredBluetoothDevice)
                }
                mutableDevicesList = devices.toList()
            }
            return@map mutableDevicesList
        }
    }

    override fun findFlipperById(deviceId: String): Flow<DiscoveredBluetoothDevice> {
        val bondedDevice = centralManager.getBondedPeripherals().firstOrNull {
            it.address == deviceId
        }
        if (bondedDevice != null) {
            return flowOf(DiscoveredBluetoothDeviceImpl(bondedDevice))
        }
        return centralManager.scan {
            Address(deviceId)
        }.distinctByPeripheral()
            .map { DiscoveredBluetoothDeviceImpl(it) }
    }

    private fun getAlreadyBondedDevices(): Flow<DiscoveredBluetoothDeviceImpl> {
        return flowOf()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return flowOf()
        }
        val ids = bluetoothAdapter.bondedDevices?.filter { device ->
            device.uuids.any { uuid -> uuid.uuid.toKotlinUuid() == Constants.BLEInformationService.SERVICE_UUID }
        }?.map {
            it.address
        } ?: return flowOf()
        return centralManager.getPeripheralsById(ids)
            .map { DiscoveredBluetoothDeviceImpl(it) }
            .asFlow()
    }

    private fun getScanDevices(): Flow<DiscoveredBluetoothDeviceImpl> {
        return centralManager.scan {
            ServiceUuid(Constants.BLEInformationService.SERVICE_UUID)
            ManufacturerData(Constants.BLEInformationService.COMPANY_ID)
        }.distinctByPeripheral()
            .map { DiscoveredBluetoothDeviceImpl(it) }
    }
}
