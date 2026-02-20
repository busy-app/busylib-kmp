package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.impl.hooks.CloudAlwaysActiveHook
import net.flipper.bridge.connection.config.impl.hooks.DeduplicateConnectionWaysHook
import net.flipper.bridge.connection.config.impl.hooks.TransactionHook
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import ru.astrainteractive.klibs.kstorage.util.save

class FDevicePersistedStorageImpl(
    private val bleConfigKrate: BleConfigSettingsKrate
) : FDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"
    private val mutex = Mutex()
    private val hooks =
        listOf<TransactionHook>(CloudAlwaysActiveHook(), DeduplicateConnectionWaysHook())

    constructor(
        observableSettings: ObservableSettings
    ) : this(BleConfigSettingsKrateImpl(observableSettings))

    override fun getCurrentDeviceFlow(): WrappedFlow<BUSYBar?> {
        return bleConfigKrate.flow.map { config ->
            val deviceId = config.currentSelectedDeviceId
            if (deviceId.isNullOrBlank()) {
                return@map null
            } else {
                config.devices.find { it.uniqueId == deviceId }
            }
        }.wrap()
    }

    override fun getAllDevicesFlow(): WrappedFlow<List<BUSYBar>> {
        return bleConfigKrate.flow.map { it.devices }.wrap()
    }

    override suspend fun transaction(
        block: PersistedStorageTransactionScope.() -> Unit
    ) = withLock(mutex, "transaction") {
        bleConfigKrate.save { original ->
            val scope = PersistedStorageTransactionScopeImpl(original)
            block(scope)
            hooks.forEach {
                with(it) {
                    scope.postTransaction()
                }
            }
            scope.get().also {
                info { "Result of transaction: $it from $original" }
            }
        }
    }

    private fun PersistedStorageTransactionScope.postTransaction() {
    }
}
