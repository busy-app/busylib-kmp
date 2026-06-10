package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.api.model.addTransport
import net.flipper.bridge.connection.config.api.model.copy
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.model.BusyCloudBar
import net.flipper.bsb.watchers.provisioning.CloudFetcherWatcher
import net.flipper.bsb.watchers.provisioning.utils.CloudFetcher
import kotlin.uuid.Uuid

internal const val DEFAULT_TEST_BAR_NAME = "Test Bar"
internal const val DEFAULT_BUSY_BAR_NAME = "BUSY Bar"

internal fun TestScope.createCloudFetcherSetup(
    devices: List<BUSYBar> = emptyList(),
    isNetworkAvailable: Boolean = true,
    networkFlow: MutableStateFlow<Boolean>? = null,
    principal: BUSYLibUserPrincipal = BUSYLibUserPrincipal.Empty,
    principalFlow: MutableStateFlow<BUSYLibUserPrincipal>? = null,
    cloudBarsResult: Result<List<BusyCloudBar>> = Result.success(emptyList())
): CloudFetcherTestSetup {
    val storage = FakePersistedStorage(MutableStateFlow(devices))
    val networkApi = FakeNetworkStateApi(networkFlow ?: MutableStateFlow(isNetworkAvailable))
    val principalApi = FakePrincipalApi(principalFlow ?: MutableStateFlow(principal))
    val cloudBarsApi = FakeCloudBarsApi(cloudBarsResult)
    val cloudFetcher = CloudFetcher(
        principalApi = principalApi,
        networkStateApi = networkApi,
        busyCloudBarsApi = cloudBarsApi,
        wsApi = FakeCloudWebSocketApi
    )

    val scope = CoroutineScope(
        SupervisorJob(backgroundScope.coroutineContext.job) +
            StandardTestDispatcher(testScheduler)
    )

    val watcher = CloudFetcherWatcher(
        scope = scope,
        persistedStorage = storage,
        cloudFetcher = cloudFetcher
    )

    return CloudFetcherTestSetup(watcher, storage, cloudBarsApi)
}

internal fun fakeToken(
    userId: Uuid = Uuid.random(),
    token: String = "token-$userId"
): BUSYLibUserPrincipal.Token = object : BUSYLibUserPrincipal.Token {
    override val userId: Uuid = userId
    override suspend fun getToken(failedToken: String?): String = token
}

internal fun cloudBar(
    id: Uuid,
    label: String?,
    hardwareId: String = "hw-$id"
): BusyCloudBar = BusyCloudBar(id = id, hardwareId = hardwareId, label = label)

internal fun busyBar(
    id: String,
    vararg connectionWays: BUSYBar.ConnectionWay,
    hardwareId: String? = null,
    humanReadableName: String = DEFAULT_TEST_BAR_NAME
): BUSYBar {
    require(connectionWays.isNotEmpty()) { "At least one connection way is required" }
    val first = connectionWays.first()
    var result = when (first) {
        is BUSYBar.ConnectionWay.BLE -> BUSYBar(
            humanReadableName = humanReadableName,
            uniqueId = id,
            ble = first
        ).copy(hardwareId = hardwareId)
        is BUSYBar.ConnectionWay.Cloud -> BUSYBar(
            humanReadableName = humanReadableName,
            hardwareId = hardwareId,
            uniqueId = id,
            cloud = first
        )
        is BUSYBar.ConnectionWay.Lan -> BUSYBar(
            humanReadableName = humanReadableName,
            hardwareId = hardwareId.orEmpty(),
            uniqueId = id,
            lan = first
        ).copy(hardwareId = hardwareId)
        is BUSYBar.ConnectionWay.Mock -> BUSYBar(
            humanReadableName = humanReadableName,
            hardwareId = hardwareId,
            uniqueId = id,
            mock = first
        )
    }
    for (way in connectionWays.drop(1)) {
        result = when (way) {
            is BUSYBar.ConnectionWay.BLE -> result.addTransport(ble = way)
            is BUSYBar.ConnectionWay.Cloud -> result.addTransport(cloud = way)
            is BUSYBar.ConnectionWay.Lan -> result.addTransport(lan = way)
            is BUSYBar.ConnectionWay.Mock -> result.addTransport(mock = way)
        }
    }
    return result
}
