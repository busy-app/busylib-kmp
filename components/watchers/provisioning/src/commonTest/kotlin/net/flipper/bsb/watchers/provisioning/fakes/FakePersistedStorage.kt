package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow

internal class FakePersistedStorage(
    val devices: MutableStateFlow<List<BUSYBar>>
) : FInternalDevicePersistedStorage {
    private val currentDeviceFlow = MutableStateFlow<BUSYBar?>(null)
    val currentDevice: BUSYBar? get() = currentDeviceFlow.value
    var transactionCount: Int = 0
        private set

    override fun getCurrentDeviceFlow() = currentDeviceFlow.asFlow().wrap()
    override fun getAllDevicesFlow() = devices.asFlow().wrap()

    override suspend fun addHook(vararg hook: TransactionHook) = Unit

    override suspend fun <T> transactionInternal(
        block: suspend InternalStorageTransactionScope.() -> T
    ): T {
        transactionCount++
        val scope = object : InternalStorageTransactionScope {
            override fun getCurrentDevice() = currentDeviceFlow.value
            override fun getAllDevices() = devices.value.toList()

            override fun setCurrentDevice(device: BUSYBar) {
                setCurrentDeviceNullable(device)
            }

            override fun setCurrentDeviceNullable(device: BUSYBar?) {
                if (device == null) {
                    currentDeviceFlow.value = null
                    return
                }
                // Mirror PersistedStorageTransactionScopeImpl: selecting an
                // unknown device adds it to storage first.
                if (devices.value.none { it.uniqueId == device.uniqueId }) {
                    addOrReplace(device)
                }
                currentDeviceFlow.value = device
            }

            override fun addOrReplace(device: BUSYBar) {
                devices.update { list ->
                    list.filter { it.uniqueId != device.uniqueId } + device
                }
            }

            override fun removeDevice(id: String) {
                devices.update { list -> list.filter { it.uniqueId != id } }
                if (currentDeviceFlow.value?.uniqueId == id) {
                    currentDeviceFlow.value = null
                }
            }
        }
        return scope.block()
    }

    override suspend fun <T> transaction(
        block: suspend PersistedStorageTransactionScope.() -> T
    ): T = transactionInternal { block() }
}
