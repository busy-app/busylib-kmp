package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbUpdateVersion
import net.flipper.bridge.connection.feature.firmwareupdate.model.FirmwareChannel
import net.flipper.bridge.connection.feature.firmwareupdate.model.SemVer
import net.flipper.bridge.connection.feature.info.api.FDeviceInfoFeatureApi
import net.flipper.bridge.connection.feature.info.model.BsbStatusFirmware
import net.flipper.bridge.connection.feature.provider.api.get
import net.flipper.bsb.cloud.rest.api.BusyFirmwareDirectoryApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.bsb.cloud.rest.model.BsbFirmwareChannelId
import net.flipper.bsb.cloud.rest.model.BsbFirmwareUpdateFileType
import net.flipper.bsb.cloud.rest.model.BsbFirmwareUpdateTarget
import net.flipper.busylib.core.wrapper.map
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn

class LanUpdateVersionProvider(
    private val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi,
    private val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi,
    private val fDeviceInfoFeatureApi: FDeviceInfoFeatureApi
) : LogTagProvider by TaggedLogger("LanUpdateVersionProvider") {

    private suspend fun getBsbFirmwareUpdateTarget(): BsbFirmwareUpdateTarget? {
        return exponentialRetry {
            fDeviceInfoFeatureApi
                .getDeviceFirmware()
                .toKotlinResult()
                .map(BsbStatusFirmware::target)
                .map { target ->
                    BsbFirmwareUpdateTarget
                        .entries
                        .find { bsbFirmwareUpdateTarget -> bsbFirmwareUpdateTarget.target == target }
                        .also { bsbFirmwareUpdateTarget ->
                            if (bsbFirmwareUpdateTarget != null) return@also
                            info { "#getBsbFirmwareUpdateTarget could not find target $target" }
                        }
                }
        }
    }

    private suspend fun requireVersionFromRestApi(
        bsbFirmwareChannelId: BsbFirmwareChannelId,
        target: BsbFirmwareUpdateTarget
    ): BsbUpdateVersion.ReadyToUpdate.Url {
        val channelId = bsbFirmwareChannelId.id
        return exponentialRetry {
            runSuspendCatching {
                val bsbFirmwareUpdateVersion = busyFirmwareDirectoryApi
                    .getFirmwareDirectory()
                    .getOrThrow()
                    .channels
                    .firstOrNull { channel -> channel.id == channelId }
                    ?.versions
                    ?.maxByOrNull { version -> version.timestamp }
                    ?: error("No $channelId version found")
                val updateFile = bsbFirmwareUpdateVersion
                    .files
                    .filter { it.target == target }
                    .firstOrNull { it.type == BsbFirmwareUpdateFileType.UPDATE_TGZ }
                    ?: error("No update file found")
                BsbUpdateVersion.ReadyToUpdate.Url(
                    version = bsbFirmwareUpdateVersion.version,
                    url = updateFile.url,
                    sha256 = updateFile.sha256,
                    changelog = bsbFirmwareUpdateVersion.changelog,
                    firmwareChannel = when (bsbFirmwareChannelId) {
                        BsbFirmwareChannelId.DEVELOPMENT -> FirmwareChannel.DEVELOPMENT
                        BsbFirmwareChannelId.RELEASE -> FirmwareChannel.RELEASE
                        BsbFirmwareChannelId.RELEASE_CANDIDATE -> FirmwareChannel.RELEASE_CANDIDATE
                    }
                )
            }.onFailure { t -> error(t) { "#requireVersionFromRestApi could not find version from REST api " } }
        }
    }

    private fun getBsbUpdateVersionUrl(
        target: BsbFirmwareUpdateTarget
    ): Flow<BsbUpdateVersion.ReadyToUpdate.Url> {
        return busyFirmwareDirectoryChannelApi
            .getChannelIdFlow()
            .map { bsbFirmwareChannelId ->
                info { "#updateVersionFlow using channel: $bsbFirmwareChannelId" }
                requireVersionFromRestApi(bsbFirmwareChannelId, target)
            }
    }

    suspend fun get(): Flow<BsbUpdateVersion> {
        val target = getBsbFirmwareUpdateTarget() ?: return flowOf(BsbUpdateVersion.FailedToCheck)
        return fDeviceInfoFeatureApi.deviceVersionFlow
            .flatMapLatest { bsbBusyBarVersion ->
                getBsbUpdateVersionUrl(target).map { urlVersion -> bsbBusyBarVersion to urlVersion }
            }
            .map { (bsbVersion, urlVersion) ->
                when (urlVersion.firmwareChannel) {
                    FirmwareChannel.DEVELOPMENT -> urlVersion

                    FirmwareChannel.RELEASE_CANDIDATE,
                    FirmwareChannel.RELEASE -> {
                        val bsbRawVersion = bsbVersion.version.replace("-rc", "")
                        val bsbSemVer = SemVer.fromString(bsbRawVersion)
                        if (bsbSemVer == null) {
                            warn { "#get could not detect bsb SemVer: $bsbRawVersion" }
                            return@map urlVersion
                        }
                        val urlRawVersion = urlVersion.version.replace("-rc", "")
                        val urlSemVer = SemVer.fromString(urlRawVersion)
                        if (urlSemVer == null) {
                            warn { "#get could not detect url SemVer: $urlRawVersion" }
                            return@map urlVersion
                        }

                        if (urlSemVer > bsbSemVer) {
                            info { "#get The new version $urlSemVer bigger than $bsbSemVer" }
                            return@map urlVersion
                        } else {
                            info { "#get The new version $urlSemVer smaller than $bsbSemVer" }
                            return@map BsbUpdateVersion.NoUpdateAvailable
                        }
                    }
                }
            }
    }
}
