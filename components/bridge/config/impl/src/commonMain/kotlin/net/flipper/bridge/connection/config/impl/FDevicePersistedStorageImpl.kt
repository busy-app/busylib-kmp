package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.impl.hooks.AlwaysActiveHook
import net.flipper.bridge.connection.config.impl.hooks.DeduplicateConnectionWaysHook
import net.flipper.bridge.connection.config.impl.hooks.TransactionHook
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLockResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import ru.astrainteractive.klibs.kstorage.util.save

class FDevicePersistedStorageImpl(
    private val bleConfigKrate: BleConfigSettingsKrate
) : FDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"
    private val mutex = Mutex()
    private val hooks = listOf<TransactionHook>(
        AlwaysActiveHook(),
        DeduplicateConnectionWaysHook()
    )

    constructor(
        observableSettings: ObservableSettings
    ) : this(BleConfigSettingsKrateImpl(observableSettings))

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
            .map { bleConfigSettings -> bleConfigSettings.devices }
            .wrap()
    }

    override suspend fun <T> transaction(
        block: suspend PersistedStorageTransactionScope.() -> T
    ): T = withLockResult(mutex, "transaction") {
        val original = bleConfigKrate.getValue()
        val scope = PersistedStorageTransactionScopeImpl(original)
        val result = block(scope)
        hooks.forEach { hook ->
            with(hook) {
                scope.postTransaction()
            }
        }

        bleConfigKrate.save(
            scope.get().also {
                info { "Result of transaction: $it from $original" }
            }
        )
        return@withLockResult result
    }
}
