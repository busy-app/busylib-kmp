package net.flipper.bridge.connection.feature.telemetry.api

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo
import net.flipper.bridge.connection.feature.telemetry.mapper.toBsbTelemetry
import net.flipper.bridge.connection.feature.telemetry.mapper.toTelemetryInfo
import net.flipper.bridge.connection.feature.telemetry.model.BsbTelemetry
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.data.semver.model.SemVer
import net.flipper.core.busylib.data.semver.util.toSemVerOrNull
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class FTelemetryFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi
) : FTelemetryFeatureApi {
    override suspend fun getTelemetry(): CResult<BsbTelemetry> {
        return rpcFeatureApi.fRpcSettingsApi
            .getTelemetry(ignoreCache = true)
            .map(TelemetryInfo::toBsbTelemetry)
            .toCResult()
    }

    override suspend fun setTelemetry(telemetry: BsbTelemetry): CResult<Unit> {
        return rpcFeatureApi.fRpcSettingsApi
            .setTelemetry(telemetry.toTelemetryInfo())
            .map { }
            .toCResult()
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.TELEMETRY)
    class Factory : FDeviceFeatureApi.Factory, LogTagProvider {
        override val TAG = "FTelemetryFeatureApi"

        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val rpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val statusSystem = exponentialRetry {
                rpcFeatureApi
                    .fRpcSystemApi
                    .getStatusSystem()
                    .onFailure { throwable ->
                        error(throwable) { "Failed to read the API version for telemetry, retrying" }
                    }
            }
            val apiVersion = statusSystem.apiSemver.toSemVerOrNull()
            if (apiVersion == null) {
                error { "Telemetry is unsupported: could not parse API version \"${statusSystem.apiSemver}\"" }
                return null
            }
            if (apiVersion < TELEMETRY_MIN_API_VERSION) {
                info { "Telemetry is unsupported: API version $apiVersion, required $TELEMETRY_MIN_API_VERSION" }
                return null
            }
            info { "Telemetry is supported: API version $apiVersion" }
            return FTelemetryFeatureApiImpl(rpcFeatureApi = rpcFeatureApi)
        }
    }

    companion object {
        // TODO(MOB-3171): the api_semver that introduces /api/telemetry is unknown; until it is set here
        //  the gate lets every bar through
        private val TELEMETRY_MIN_API_VERSION = SemVer(
            majorVersion = 0,
            minorVersion = 0,
            patchVersion = 0,
            additionalVersion = 0
        )
    }
}
