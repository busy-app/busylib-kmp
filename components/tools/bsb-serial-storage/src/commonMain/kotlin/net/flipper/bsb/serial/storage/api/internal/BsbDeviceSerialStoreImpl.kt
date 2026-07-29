package net.flipper.bsb.serial.storage.api.internal

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.first
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bsb.serial.storage.api.BsbDeviceSerialStore
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider

@Inject
@ContributesBinding(BusyLibGraph::class, binding<BsbDeviceSerialStore>())
class BsbDeviceSerialStoreImpl(
    private val fDevicePersistedStorage: FDevicePersistedStorage
) : BsbDeviceSerialStore, LogTagProvider {
    override val TAG = "BsbDeviceSerialStore"

    override suspend fun findSerial(deviceUniqueId: String): String? {
        return fDevicePersistedStorage.getAllDevicesFlow()
            .first()
            .firstOrNull { busyBar -> busyBar.uniqueId == deviceUniqueId }
            ?.serialNumber
    }
}
