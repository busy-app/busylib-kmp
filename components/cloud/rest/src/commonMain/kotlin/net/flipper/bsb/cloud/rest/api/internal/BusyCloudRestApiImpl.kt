package net.flipper.bsb.cloud.rest.api.internal

import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.cloud.rest.api.BusyCloudRestApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, BusyCloudRestApi::class)
class BusyCloudRestApiImpl(
    override val barsApi: BusyCloudBarsApi
) : BusyCloudRestApi
