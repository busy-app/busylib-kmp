package net.flipper.bridge.connection.feature.link.check.onready.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.link.check.ondemand.api.FLinkedInfoOnDemandFeatureApi
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.critical.FRpcCriticalFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.RpcLinkedAccountInfo
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibBarsApi
import net.flipper.busylib.core.wrapper.WrappedFlow
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
    private val busyLibBarsApi: BUSYLibBarsApi
) : FLinkedInfoOnReadyFeatureApi, FLinkedInfoOnDemandFeatureApi, LogTagProvider {
    override val TAG = "FLinkedInfoOnReadyFeatureApi"
    private val _status = MutableStateFlow<LinkedAccountInfo?>(null)
    override val status: WrappedFlow<LinkedAccountInfo> = _status.filterNotNull().wrap()
    private val singleJobScope = scope.asSingleJobScope()

    override suspend fun onReady() {
        busyLibPrincipalApi.getPrincipalFlow()
            .filter { it !is BUSYLibUserPrincipal.Loading }
            .filter { it !is BUSYLibUserPrincipal.Token.Impl }
            .onEach { _ -> tryCheckLinkedInfo() }
            .launchIn(scope)
    }

    private fun RpcLinkedAccountInfo.asSealed(currentUserEmail: Uuid?): LinkedAccountInfo {
        info { "Calculate state from $this and current user $currentUserEmail" }
        val linkedUuid = this.userId
        return when {
            // BUSY Bar is not linked
            !linked -> LinkedAccountInfo.NotLinked
            // BUSY Bar is linked, but userId is missing
            userId == null -> LinkedAccountInfo.Error
            // BUSY Bar is linked to the current user
            linkedUuid != null && userId == currentUserEmail ->
                LinkedAccountInfo.Linked.SameUser(linkedUuid)
            // BUSY Bar is linked to a different user
            linkedUuid != null && currentUserEmail != null ->
                LinkedAccountInfo.Linked.DifferentUser(linkedUuid)
            // BUSY Bar is linked, but we don't know the current user
            linkedUuid != null && currentUserEmail == null ->
                LinkedAccountInfo.Linked.MissingBusyCloud(linkedUuid)
            // Fallback case
            else -> LinkedAccountInfo.Error
        }
    }

    private fun tryCheckLinkedInfo() {
        singleJobScope.launch(SingleJobMode.CANCEL_PREVIOUS) {
            val principal = busyLibPrincipalApi.getPrincipalFlow()
                .filter { principal -> principal !is BUSYLibUserPrincipal.Loading }
                .first() as? BUSYLibUserPrincipal.Full

            info { "Local principal is ${principal?.userId}/${principal?.email}" }

            val info = exponentialRetry {
                rpcFeatureApi.invalidateLinkedUser(principal?.userId)
                    .map { result -> result.asSealed(principal?.userId) }
            }

            info { "BUSY Bar info is $info" }
            if (info is LinkedAccountInfo.NotLinked && principal != null) {
                info { "Start authorization for BUSY Bar..." }
                authBusyBar(principal).onFailure {
                    error(it) { "Failed authorize for BUSY Bar" }
                }
            }
            _status.emit(info)
        }
    }

    private suspend fun authBusyBar(
        principal: BUSYLibUserPrincipal.Full
    ): Result<Unit> = runCatching {
        val linkCode = rpcFeatureApi.getLinkCode().getOrThrow()

        info { "Receive link code from BUSY Bar: $linkCode" }

        busyLibBarsApi.registerBusyBar(principal, linkCode.code).getOrThrow()

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

    override suspend fun deleteAccount(): Result<SuccessResponse> {
        return rpcFeatureApi.deleteAccount()
            .onSuccess { tryCheckLinkedInfo() }
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
