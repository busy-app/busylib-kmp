package net.flipper.bsb.cloud.rest.api

import me.tatarka.inject.annotations.Inject
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyCloudRestApi::class)
class BusyCloudRestApiImpl(
    override val barsApi: BusyCloudBarsApi,
    override val busyFirmwareDirectoryApi: BusyFirmwareDirectoryApi
) : BusyCloudRestApi
