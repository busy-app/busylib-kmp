package net.flipper.bsb.cloud.rest.model

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Inject
import net.flipper.bsb.cloud.api.BUSYLibHostApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.ktor.getHttpClient
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
class InternalCloudNetworkContext(
    val bsbHostApi: BUSYLibHostApi
) {
    val networkDispatcher: CoroutineDispatcher = FlipperDispatchers.default
    val httpClient: HttpClient = getHttpClient()
}
