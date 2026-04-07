package net.flipper.bridge.connection.feature.link.check.onready.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.map
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Inject
class FLinkInfoOnReadyFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcCriticalFeatureApi,
    @Assisted private val scope: CoroutineScope,
    private val busyLibPrincipalApi: BUSYLibPrincipalApi,
    private val busyLibBarsApi: BusyCloudBarsApi
) : FLinkedInfoOnReadyFeatureApi, FLinkedInfoOnDemandFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoOnReadyFeatureApi"
    private val _status = MutableStateFlow<LinkedAccountInfo?>(null)
    override val status: WrappedFlow<LinkedAccountInfo> = _status.filterNotNull().wrap()
    private val singleJobScope = scope.asSingleJobScope()

    override suspend fun onReady() {
        busyLibPrincipalApi.getPrincipalFlow()
            .filter { it !is BUSYLibUserPrincipal.Loading }
            .map { it as? BUSYLibUserPrincipal.Token }
            .onEach { principal -> tryCheckLinkedInfo(principal) }
            .launchIn(scope)
    }

    private fun RpcLinkedAccountInfo.asSealed(currentUserId: Uuid?): LinkedAccountInfo {
        info { "Calculate state from $this and current user $currentUserId" }
        val linkedUuid = this.userId
        val linkedMail = this.email

        return when {
            // BUSY Bar is not linked
            !linked -> LinkedAccountInfo.NotLinked
            // BUSY Bar is linked, but userId is missing
            linkedUuid == null || linkedMail == null -> LinkedAccountInfo.Error
            // BUSY Bar is linked to the current user
            linkedUuid == currentUserId ->
                LinkedAccountInfo.Linked.SameUser(linkedUuid, linkedMail)
            // BUSY Bar is linked to a different user
            currentUserId != null ->
                LinkedAccountInfo.Linked.DifferentUser(linkedUuid, linkedMail)
            // BUSY Bar is linked, but we don't know the current user
            // Fallback case
            else -> LinkedAccountInfo.Linked.MissingBusyCloud(linkedUuid, linkedMail)
        }
    }

    private fun tryCheckLinkedInfo(principal: BUSYLibUserPrincipal.Token?) {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            info { "Local principal is ${principal?.userId}" }

            val info = exponentialRetry {
                rpcFeatureApi.invalidateLinkedUser(principal?.userId)
                    .map { result -> result.asSealed(principal?.userId) }
            }

            info { "BUSY Bar info is $info" }
            if (info is LinkedAccountInfo.NotLinked && principal != null) {
                info { "Start authorization for BUSY Bar..." }
                exponentialRetry {
                    authBusyBar(principal)
                        .onFailure { t ->
                            error(t) { "Failed authorize for BUSY Bar" }
                        }
                }
            }
            _status.emit(info)
        }
    }

    private suspend fun authBusyBar(
        principal: BUSYLibUserPrincipal.Token
    ): Result<Unit> = runCatching {
        val linkCode = rpcFeatureApi.getLinkCode().getOrThrow()

        info { "Receive link code from BUSY Bar: $linkCode" }

        busyLibBarsApi.linkBusyBar(principal, linkCode.code).getOrThrow()

        info { "BUSY Bar registered, waiting user registration from BUSY Bar" }

        var busyBarUser: RpcLinkedAccountInfo

        withTimeout(ACCOUNT_PROVIDING_TIMEOUT) {
            do {
                busyBarUser = rpcFeatureApi.invalidateLinkedUser(principal.userId).getOrThrow()
                info { "Requested BUSY Bar user info: $busyBarUser" }
            } while (principal.userId != busyBarUser.userId)
        }

        info { "Completed authorization for BUSY Bar" }
    }

    override suspend fun deleteAccount(): CResult<Unit> {
        return rpcFeatureApi.deleteAccount()
            .onSuccess {
                tryCheckLinkedInfo(
                    principal = busyLibPrincipalApi
                        .getPrincipalFlow()
                        .filter { it !is BUSYLibUserPrincipal.Loading }
                        .first() as? BUSYLibUserPrincipal.Token
                )
            }
            .map { }
            .toCResult()
    }

    @Inject
    class InternalFactory(
        private val factory: (
            FRpcCriticalFeatureApi,
            CoroutineScope,
        ) -> FLinkInfoOnReadyFeatureApiImpl
    ) {
        operator fun invoke(
            rpcFeatureApi: FRpcCriticalFeatureApi,
            scope: CoroutineScope,
        ): FLinkInfoOnReadyFeatureApiImpl = factory(
            rpcFeatureApi,
            scope,
        )
    }

    companion object {
        private val ACCOUNT_PROVIDING_TIMEOUT = 3.seconds
    }
}
