package net.flipper.bridge.connection.feature.telemetry.api.fake

import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.model.AudioVolumeInfo
import net.flipper.bridge.connection.feature.rpc.api.model.DisplayBrightnessInfo
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo

internal class FakeRpcSettingsApi(
    private val getTelemetryResult: Result<TelemetryInfo>,
    private val setTelemetryResult: Result<SuccessResponse>
) : FRpcSettingsApi {
    val setTelemetryRequests = mutableListOf<TelemetryInfo>()

    override suspend fun getTelemetry(ignoreCache: Boolean): Result<TelemetryInfo> {
        return getTelemetryResult
    }

    override suspend fun setTelemetry(body: TelemetryInfo): Result<SuccessResponse> {
        setTelemetryRequests += body
        return setTelemetryResult
    }

    override suspend fun getName(ignoreCache: Boolean): Result<NameInfo> = error("not used")

    override suspend fun setName(body: NameInfo): Result<SuccessResponse> = error("not used")

    override suspend fun getDisplayBrightness(ignoreCache: Boolean): Result<DisplayBrightnessInfo> =
        error("not used")

    override suspend fun setDisplayBrightness(
        brightnessInfo: DisplayBrightnessInfo
    ): Result<SuccessResponse> = error("not used")

    override suspend fun getAudioVolume(ignoreCache: Boolean): Result<AudioVolumeInfo> =
        error("not used")

    override suspend fun setAudioVolume(volume: Int): Result<SuccessResponse> = error("not used")
}
