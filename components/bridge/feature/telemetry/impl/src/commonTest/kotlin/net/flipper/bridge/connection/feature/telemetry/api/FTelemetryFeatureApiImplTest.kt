@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.telemetry.api

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarStatusSystem
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo
import net.flipper.bridge.connection.feature.telemetry.api.fake.FakeConnectedDeviceApi
import net.flipper.bridge.connection.feature.telemetry.api.fake.FakeRpcFeatureApi
import net.flipper.bridge.connection.feature.telemetry.api.fake.FakeRpcSettingsApi
import net.flipper.bridge.connection.feature.telemetry.api.fake.FakeRpcSystemApi
import net.flipper.bridge.connection.feature.telemetry.api.fake.FakeUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.telemetry.model.BsbTelemetry
import net.flipper.busylib.core.wrapper.CResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Instant

class FTelemetryFeatureApiImplTest {

    private fun statusSystem(apiSemver: String): Result<BusyBarStatusSystem> {
        return Result.success(
            BusyBarStatusSystem(
                apiSemver = apiSemver,
                uptime = Duration.ZERO,
                bootTime = Instant.fromEpochSeconds(0)
            )
        )
    }

    private fun rpcFeatureApi(
        systemApi: FakeRpcSystemApi,
        settingsApi: FakeRpcSettingsApi = FakeRpcSettingsApi(
            getTelemetryResult = Result.success(TelemetryInfo(enabled = false, asked = false)),
            setTelemetryResult = Result.success(SuccessResponse("OK"))
        )
    ): FakeRpcFeatureApi {
        return FakeRpcFeatureApi(
            fRpcSystemApi = systemApi,
            fRpcSettingsApi = settingsApi
        )
    }

    private suspend fun TestScope.createFeature(rpcFeatureApi: FakeRpcFeatureApi?): FDeviceFeatureApi? {
        return FTelemetryFeatureApiImpl.Factory().invoke(
            unsafeFeatureDeviceApi = FakeUnsafeDeviceFeatureApi(rpcFeatureApi),
            scope = backgroundScope,
            connectedDevice = FakeConnectedDeviceApi()
        )
    }

    @Test
    fun GIVEN_no_rpc_feature_WHEN_factory_invoked_THEN_returns_null(): TestResult {
        return runTest {
            val feature = createFeature(rpcFeatureApi = null)

            assertNull(feature)
        }
    }

    @Test
    fun GIVEN_supported_api_version_WHEN_factory_invoked_THEN_returns_feature(): TestResult {
        return runTest {
            val systemApi = FakeRpcSystemApi(listOf(statusSystem("6.3.0")))

            val feature = createFeature(rpcFeatureApi(systemApi))

            assertIs<FTelemetryFeatureApi>(feature)
        }
    }

    @Test
    fun GIVEN_version_read_fails_twice_WHEN_factory_invoked_THEN_read_is_retried_and_feature_returned(): TestResult {
        return runTest {
            val systemApi = FakeRpcSystemApi(
                listOf(
                    Result.failure(IllegalStateException("first")),
                    Result.failure(IllegalStateException("second")),
                    statusSystem("6.3.0")
                )
            )

            val feature = createFeature(rpcFeatureApi(systemApi))

            assertIs<FTelemetryFeatureApi>(feature)
            assertEquals(3, systemApi.getStatusSystemCalls)
        }
    }

    @Test
    fun GIVEN_unparseable_api_version_WHEN_factory_invoked_THEN_returns_null(): TestResult {
        return runTest {
            val systemApi = FakeRpcSystemApi(listOf(statusSystem("unknown")))

            val feature = createFeature(rpcFeatureApi(systemApi))

            assertNull(feature)
        }
    }

    @Test
    fun GIVEN_rpc_returns_telemetry_WHEN_get_telemetry_THEN_success_with_mapped_value(): TestResult {
        return runTest {
            val settingsApi = FakeRpcSettingsApi(
                getTelemetryResult = Result.success(TelemetryInfo(enabled = true, asked = false)),
                setTelemetryResult = Result.success(SuccessResponse("OK"))
            )
            val feature = createFeature(
                rpcFeatureApi(
                    systemApi = FakeRpcSystemApi(listOf(statusSystem("6.3.0"))),
                    settingsApi = settingsApi
                )
            )
            assertIs<FTelemetryFeatureApi>(feature)

            val result = feature.getTelemetry()

            assertEquals(CResult.Success(BsbTelemetry(enabled = true, asked = false)), result)
        }
    }

    @Test
    fun GIVEN_supported_bar_WHEN_set_telemetry_THEN_rpc_receives_mapped_value_and_result_is_success(): TestResult {
        return runTest {
            val settingsApi = FakeRpcSettingsApi(
                getTelemetryResult = Result.success(TelemetryInfo(enabled = false, asked = false)),
                setTelemetryResult = Result.success(SuccessResponse("OK"))
            )
            val feature = createFeature(
                rpcFeatureApi(
                    systemApi = FakeRpcSystemApi(listOf(statusSystem("6.3.0"))),
                    settingsApi = settingsApi
                )
            )
            assertIs<FTelemetryFeatureApi>(feature)

            val result = feature.setTelemetry(BsbTelemetry(enabled = false, asked = true))

            assertEquals(
                listOf(TelemetryInfo(enabled = false, asked = true)),
                settingsApi.setTelemetryRequests
            )
            assertIs<CResult.Success<Unit>>(result)
        }
    }
}
