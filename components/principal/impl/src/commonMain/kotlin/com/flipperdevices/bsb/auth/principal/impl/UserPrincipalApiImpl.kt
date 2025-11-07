package com.flipperdevices.bsb.auth.principal.impl

import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipal
import com.flipperdevices.bsb.auth.principal.api.BsbUserPrincipalApi
import com.flipperdevices.bsb.auth.principal.preference.BsbUserPrincipalKrate
import com.flipperdevices.core.di.AppGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppGraph::class)
@ContributesBinding(AppGraph::class, binding<BsbUserPrincipalApi>())
class UserPrincipalApiImpl(
    private val bsbUserKrate: BsbUserPrincipalKrate,
    private val coroutineScope: CoroutineScope
) : BsbUserPrincipalApi {

    override fun getPrincipalFlow() = bsbUserKrate.stateFlow(coroutineScope)

    override suspend fun <T> withTokenPrincipal(
        block: suspend (BsbUserPrincipal.Token) -> Result<T>
    ): Result<T> {
        val principal = getPrincipalFlow()
            .filterNotNull()
            .first()
        if (principal !is BsbUserPrincipal.Token) {
            return Result.failure(IllegalStateException("User not found"))
        }
        return block(principal)
    }

    override fun setPrincipal(principal: BsbUserPrincipal) {
        coroutineScope.launch { bsbUserKrate.save(principal) }
    }
}
