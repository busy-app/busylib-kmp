package net.flipper.busylib

import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.device.firmwareupdate.updater.api.FirmwareUpdaterApi
import net.flipper.bsb.cloud.rest.channel.api.BusyFirmwareDirectoryChannelApi
import net.flipper.eventbus.api.EventBusApi
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.DrawToolSyncApi
import net.flipper.tools.multistream.api.MultiStreamApi

interface BUSYLib {
    val connectionService: FConnectionService
    val orchestrator: FDeviceOrchestrator
    val featureProvider: FFeatureProvider
    val firmwareUpdaterApi: FirmwareUpdaterApi
    val persistedStorage: FDevicePersistedStorage
    val multiStreamApi: MultiStreamApi
    val drawToolStatusesApi: DrawToolStatusesApi
    val drawToolSyncApi: DrawToolSyncApi
    val busyFirmwareDirectoryChannelApi: BusyFirmwareDirectoryChannelApi
    val eventBus: EventBusApi

    fun launch()
}
