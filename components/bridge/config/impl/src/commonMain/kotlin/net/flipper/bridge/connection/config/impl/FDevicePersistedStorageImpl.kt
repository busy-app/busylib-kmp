package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.impl.hooks.AlwaysActiveHook
import net.flipper.bridge.connection.config.impl.hooks.RemoveDuplicateCloudHook
import net.flipper.bridge.connection.config.impl.hooks.RemoveDuplicateHardwareIdHook
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.config.internal.InternalStorageTransactionScope
import net.flipper.bridge.connection.config.internal.TransactionHook
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.ktx.common.withLockResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FInternalDevicePersistedStorage::class)
@ContributesBinding(BusyLibGraph::class, FDevicePersistedStorage::class)
class FDevicePersistedStorageImpl(
    observableSettings: ObservableSettings
) : FInternalDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"
    private val mutex = Mutex()

    private val bleConfigKrate = BBConfigSettingsKrateImpl(observableSettings, logger = this)
    private var hooks = listOf<TransactionHook>(
        AlwaysActiveHook(),
        RemoveDuplicateCloudHook(),
        RemoveDuplicateHardwareIdHook()
    ).sortedBy { it.getPriority() }

    override suspend fun addHook(vararg hook: TransactionHook) {
        withLock(mutex, "add_hook") {
            hooks = hooks.plus(hook).sortedBy { it.getPriority() }
        }
    }

    override fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?> {
        return bleConfigKrate.flow.map { config ->
            val deviceId = config.currentSelectedDeviceId
            if (deviceId.isNullOrBlank()) {
                return@map null
            } else {
                config.devices.find { busyBar -> busyBar.uniqueId == deviceId }
            }
        }.wrap()
    }

    override fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>> {
        return bleConfigKrate.flow
            .map { bbConfigSettings -> bbConfigSettings.devices }
            .wrap()
    }

    override suspend fun <T> transactionInternal(
        block: suspend InternalStorageTransactionScope.() -> T
    ): T = withLockResult(mutex, "transaction") {
        val original = bleConfigKrate.getValue()
        val scope = PersistedStorageTransactionScopeImpl(original)
        val result = block(scope)
        hooks.forEach { hook ->
            with(hook) {
                scope.postTransaction()
            }
        }
        val toSave = scope.get()
        if (original == toSave) {
            info { "No changes, so skip current object: $original" }
        } else {
            info { "Result of transaction: $toSave from $original" }
            bleConfigKrate.save(toSave)
        }

        return@withLockResult result
    }

    override suspend fun <T> transaction(
        block: suspend PersistedStorageTransactionScope.() -> T
    ): T = transactionInternal { block() }
}
