package net.flipper.bridge.device.firmwareupdate.updater.api

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.model.BsbBusyBarVersion
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.device.firmwareupdate.updater.model.BusyBarVersionTransition
import net.flipper.bridge.device.firmwareupdate.updater.model.FwUpdateState
import net.flipper.core.busylib.ktx.common.getOrNull
import net.flipper.core.busylib.ktx.common.orNullable
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.verbose

@Inject
class PreviousVersionFlowProvider(
    private val fFeatureProvider: FFeatureProvider,
    private val fDevicePersistedStorage: FDevicePersistedStorage,
) : LogTagProvider by TaggedLogger("PreviousVersionFlowProvider") {

    private fun getVersionFlow(): Flow<BsbBusyBarVersion?> {
        return fFeatureProvider.get<FDeviceInfoFeatureApi>()
            .map { status -> status.tryCast<FFeatureStatus.Supported<FDeviceInfoFeatureApi>>() }
            .flatMapLatest { status -> status?.featureApi?.deviceVersionFlow.orNullable() }
            .map { bsbBusyBarVersion -> bsbBusyBarVersion?.version?.let(::BsbBusyBarVersion) }
    }

    internal fun getPreviousVersionFlow(fwUpdateFlow: Flow<FwUpdateState>) = channelFlow(
        block = {
            // Reset versionTransition on BusyBar change
            while (currentCoroutineContext().isActive) {
                fDevicePersistedStorage.getCurrentDeviceFlow()
                    .onStart { send(null) }
                    .distinctUntilChangedBy { busyBar -> busyBar?.uniqueId }
                    .onEach { send(null) }
                    .mapLatest {
                        val beforeUpdateVersion = getVersionFlow()
                            .filterNotNull()
                            .first()
                        verbose { "#getPreviousVersionFlow beforeUpdateVersion: $beforeUpdateVersion" }
                        send(
                            BusyBarVersionTransition(
                                previousVersion = null,
                                currentVersion = beforeUpdateVersion
                            )
                        )
                        val batteryLowStateDeferred = async {
                            fwUpdateFlow
                                .filterIsInstance<FwUpdateState.BatteryLow>()
                                .first()
                        }
                        fwUpdateFlow.filterIsInstance<FwUpdateState.Updating>().first()
                        fwUpdateFlow.filter { state -> state !is FwUpdateState.Updating }.first()
                        if (batteryLowStateDeferred.getOrNull() == null) {
                            val afterUpdateVersion = getVersionFlow().filterNotNull().first()
                            verbose { "#getPreviousVersionFlow afterUpdateVersion: $afterUpdateVersion" }
                            send(
                                BusyBarVersionTransition(
                                    previousVersion = beforeUpdateVersion,
                                    currentVersion = afterUpdateVersion
                                )
                            )
                        }
                    }
                    .first()
            }
        }
    )
}
