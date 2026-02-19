package net.flipper.bridge.connection.config.impl

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import ru.astrainteractive.klibs.kstorage.util.save

class FDevicePersistedStorageImpl(
    private val bleConfigKrate: BleConfigSettingsKrate
) : FDevicePersistedStorage, LogTagProvider {
    override val TAG = "FDevicePersistedStorage"
    private val mutex = Mutex()

    constructor(
        observableSettings: ObservableSettings
    ) : this(BleConfigSettingsKrateImpl(observableSettings))

    override fun getCurrentDeviceFlow(): Flow<BUSYBar?> {
        return bleConfigKrate.flow.map { config ->
            val deviceId = config.currentSelectedDeviceId
            if (deviceId.isNullOrBlank()) {
                return@map null
            } else {
                config.devices.find { it.uniqueId == deviceId }
            }
        }
    }

    override fun getAllDevicesFlow(): Flow<List<BUSYBar>> {
        return bleConfigKrate.flow.map { it.devices }
    }

    override suspend fun transaction(
        block: PersistedStorageTransactionScope.() -> Unit
    ) = withLock(mutex, "transaction") {
        bleConfigKrate.save { original ->
            val scope = PersistedStorageTransactionScopeImpl(original)
            block(scope)
            scope.get().also {
                info { "Result of transaction: $it from $original" }
            }
        }
    }
}
