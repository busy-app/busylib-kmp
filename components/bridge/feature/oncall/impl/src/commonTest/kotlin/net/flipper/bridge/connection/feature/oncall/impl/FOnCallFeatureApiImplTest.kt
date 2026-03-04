package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWebSocketApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class FOnCallFeatureApiImplTest {

    @Test
    fun GIVEN_start_WHEN_time_advances_THEN_draw_called_periodically() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val scope = CoroutineScope(coroutineContext + Job())
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, scope)

        launch { api.start() }
        advanceTimeBy(1) // let first draw happen

        assertEquals(1, fakeAssetsApi.drawRequests.size)

        advanceTimeBy(3.seconds)
        assertEquals(2, fakeAssetsApi.drawRequests.size)

        advanceTimeBy(3.seconds)
        assertEquals(3, fakeAssetsApi.drawRequests.size)

        api.stop()
    }

    @Test
    fun GIVEN_start_WHEN_stop_THEN_remove_draw_called() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val scope = CoroutineScope(coroutineContext + Job())
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, scope)

        launch { api.start() }
        advanceTimeBy(1)

        api.stop()
        advanceTimeBy(1)

        assertEquals("on_call", fakeAssetsApi.removedAppIds.last())
    }

    @Test
    fun GIVEN_started_WHEN_start_again_THEN_previous_cancelled_and_remove_draw_called() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val scope = CoroutineScope(coroutineContext + Job())
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, scope)

        launch { api.start() }
        advanceTimeBy(1)
        assertEquals(1, fakeAssetsApi.drawRequests.size)
        assertEquals(0, fakeAssetsApi.removedAppIds.size)

        // start again — should cancel previous (triggering removeDraw) and restart
        launch { api.start() }
        advanceTimeBy(1)

        assertTrue(fakeAssetsApi.removedAppIds.isNotEmpty())
        assertEquals("on_call", fakeAssetsApi.removedAppIds.last())

        api.stop()
    }

    @Test
    fun GIVEN_start_WHEN_draw_called_THEN_request_has_correct_params() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val scope = CoroutineScope(coroutineContext + Job())
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, scope)

        launch { api.start() }
        advanceTimeBy(1)

        val request = fakeAssetsApi.drawRequests.first()
        assertEquals("on_call", request.appId)

        val element = request.elements.first()
        assertEquals("0", element.id)
        assertEquals(30, element.timeout)
        assertEquals(50, element.priority)
        assertEquals(DrawRequest.Display.FRONT, element.display)
        assertEquals(DrawRequest.Element.ElementType.ANIM, element.type)
        assertEquals("shared/on_call_72x16", element.builtinAnim)
        assertEquals("loop", element.section)
        assertEquals(true, element.loop)

        api.stop()
    }
}

private class FakeRpcAssetsApi : FRpcAssetsApi {
    val drawRequests = mutableListOf<DrawRequest>()
    val removedAppIds = mutableListOf<String>()

    override suspend fun uploadAsset(
        appId: String,
        file: String,
        content: ByteArray
    ): Result<SuccessResponse> {
        return Result.success(SuccessResponse("ok"))
    }

    override suspend fun displayDraw(request: DrawRequest): Result<SuccessResponse> {
        drawRequests.add(request)
        return Result.success(SuccessResponse("ok"))
    }

    override suspend fun removeDraw(appId: String): Result<SuccessResponse> {
        removedAppIds.add(appId)
        return Result.success(SuccessResponse("ok"))
    }
}

private class FakeRpcFeatureApi(
    override val fRpcAssetsApi: FRpcAssetsApi
) : FRpcFeatureApi {
    override val fRpcSystemApi: FRpcSystemApi get() = error("Not used")
    override val fRpcWifiApi: FRpcWifiApi get() = error("Not used")
    override val fRpcBleApi: FRpcBleApi get() = error("Not used")
    override val fRpcSettingsApi: FRpcSettingsApi get() = error("Not used")
    override val fRpcStreamingApi: FRpcStreamingApi get() = error("Not used")
    override val fRpcUpdaterApi: FRpcUpdaterApi get() = error("Not used")
    override val fRpcMatterApi: FRpcMatterApi get() = error("Not used")
    override val fRpcTimeZoneApi: FRpcTimeZoneApi get() = error("Not used")
    override val fRpcWebSocketApi: FRpcWebSocketApi get() = error("Not used")
}
