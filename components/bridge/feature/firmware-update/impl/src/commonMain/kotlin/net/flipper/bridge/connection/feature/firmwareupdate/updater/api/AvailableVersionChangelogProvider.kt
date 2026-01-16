package net.flipper.bridge.connection.feature.firmwareupdate.updater.api

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.tryCast

@Inject
class AvailableVersionChangelogProvider(
    private val fFeatureProvider: FFeatureProvider,
) {

    fun getLatestAvailableChangelogFlow() = fFeatureProvider.get<FFirmwareUpdateFeatureApi>()
        .map { status ->
            status
                .tryCast<FFeatureStatus.Supported<FFirmwareUpdateFeatureApi>>()
                ?.featureApi
        }
        .flatMapLatest { featureApi ->
            if (featureApi == null) return@flatMapLatest flowOf(null)
            featureApi.getUpdateStatusFlow()
                .filter { status ->
                    when (status.check.status) {
                        UpdateStatus.Check.CheckResult.NOT_AVAILABLE,
                        UpdateStatus.Check.CheckResult.AVAILABLE -> true

                        UpdateStatus.Check.CheckResult.NONE,
                        UpdateStatus.Check.CheckResult.FAILURE -> false
                    }
                }
                .map { status -> status.check.availableVersion }
                .distinctUntilChanged()
                .mapLatest { version ->
                    if (version.isEmpty()) {
                        null
                    } else {
                        exponentialRetry {
                            featureApi.getVersionChangelog(version)
                        }
                    }
                }
        }
}
