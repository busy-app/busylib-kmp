package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.oncall.api.model.OnCallAnimation
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBleApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcBusyApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSettingsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStreamingApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcSystemApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcTimeZoneApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcUpdaterApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcWifiApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.api.model.SuccessResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FOnCallFeatureApiImplTest {

    @Test
    fun GIVEN_start_WHEN_time_advances_THEN_draw_called_periodically() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1) // let first draw happen

        assertEquals(1, fakeAssetsApi.drawRequests.size)

        advanceTimeBy(10.seconds)
        assertEquals(2, fakeAssetsApi.drawRequests.size)

        advanceTimeBy(10.seconds)
        assertEquals(3, fakeAssetsApi.drawRequests.size)

        api.stop()
    }

    @Test
    fun GIVEN_start_WHEN_stop_THEN_remove_draw_called() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)

        api.stop()
        advanceTimeBy(1)

        assertEquals("busy_lib_on_call", fakeAssetsApi.removedAppIds.last())
    }

    @Test
    fun GIVEN_started_WHEN_start_again_THEN_skip_if_running() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)
        assertEquals(1, fakeAssetsApi.drawRequests.size)
        assertEquals(0, fakeAssetsApi.removedAppIds.size)

        // start again — SKIP_IF_RUNNING means second start is a no-op
        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)

        // No removeDraw called since previous job was not cancelled
        assertEquals(0, fakeAssetsApi.removedAppIds.size)
        // Still only 1 draw (second start was skipped)
        assertEquals(1, fakeAssetsApi.drawRequests.size)

        api.stop()
    }

    @Test
    fun GIVEN_start_WHEN_draw_called_THEN_request_has_correct_params() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val fakeRpcFeatureApi = FakeRpcFeatureApi(fakeAssetsApi)
        val api = FOnCallFeatureApiImpl(fakeRpcFeatureApi, backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)

        val request = fakeAssetsApi.drawRequests.first()
        assertEquals("busy_lib_on_call", request.appId)
        assertEquals(50, request.priority)

        val element = request.elements.first()
        assertEquals("busy_lib_on_call_anim", element.id)
        assertEquals(DrawRequest.Display.FRONT, element.display)
        assertEquals(DrawRequest.Element.ElementType.ANIMATION, element.type)
        assertEquals("shared/on_call_72x16.anim", element.stockPath)
        assertEquals(true, element.loop)

        api.stop()
    }

    @Test
    fun GIVEN_each_animation_WHEN_start_THEN_draw_uses_firmware_stock_path() = runTest {
        val expectedPaths = mapOf(
            OnCallAnimation.KEEP_OUT to "shared/keep_out_72x16.anim",
            OnCallAnimation.DND to "shared/dnd_72x16.anim",
            OnCallAnimation.MEETING to "shared/meeting_72x16.anim",
            OnCallAnimation.ON_CALL to "shared/on_call_72x16.anim",
            OnCallAnimation.LUNCH to "shared/lunch_72x16.anim",
            OnCallAnimation.BACK_SOON to "shared/back_soon_72x16.anim",
            OnCallAnimation.BOOKED to "shared/booked_72x16.anim",
            OnCallAnimation.FLOW to "shared/flow_72x16.anim",
            OnCallAnimation.CHILL_TIME to "shared/chill_time_72x16.anim",
            OnCallAnimation.ON_AIR to "shared/on_air_72x16.anim",
            OnCallAnimation.CODING to "shared/coding_72x16.anim",
            OnCallAnimation.LOW_SOCIAL_BATTERY to "shared/low_social_battery_72x16.anim"
        )
        assertEquals(OnCallAnimation.entries.toSet(), expectedPaths.keys)

        expectedPaths.forEach { (animation, path) ->
            val fakeAssetsApi = FakeRpcAssetsApi()
            val api = FOnCallFeatureApiImpl(FakeRpcFeatureApi(fakeAssetsApi), backgroundScope)

            api.start(animation)
            advanceTimeBy(1)

            assertEquals(path, fakeAssetsApi.drawRequests.single().elements.single().stockPath)

            api.stop()
            advanceTimeBy(1)
        }
    }

    @Test
    fun GIVEN_started_WHEN_start_with_other_animation_THEN_draw_switches_without_remove() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val api = FOnCallFeatureApiImpl(FakeRpcFeatureApi(fakeAssetsApi), backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)

        api.start(OnCallAnimation.MEETING)
        advanceTimeBy(1)

        assertEquals(
            listOf("shared/on_call_72x16.anim", "shared/meeting_72x16.anim"),
            fakeAssetsApi.drawRequests.map { request -> request.elements.single().stockPath }
        )
        assertEquals(0, fakeAssetsApi.removedAppIds.size)

        advanceTimeBy(10.seconds)
        assertEquals("shared/meeting_72x16.anim", fakeAssetsApi.drawRequests.last().elements.single().stockPath)

        api.stop()
        advanceTimeBy(1)

        assertEquals(listOf("busy_lib_on_call"), fakeAssetsApi.removedAppIds)
    }

    @Test
    fun GIVEN_stopped_WHEN_start_again_THEN_draw_resumes_with_new_animation() = runTest {
        val fakeAssetsApi = FakeRpcAssetsApi()
        val api = FOnCallFeatureApiImpl(FakeRpcFeatureApi(fakeAssetsApi), backgroundScope)

        api.start(OnCallAnimation.ON_CALL)
        advanceTimeBy(1)
        api.stop()
        advanceTimeBy(1)

        api.start(OnCallAnimation.CODING)
        advanceTimeBy(1)

        assertEquals("shared/coding_72x16.anim", fakeAssetsApi.drawRequests.last().elements.single().stockPath)
        assertEquals(2, fakeAssetsApi.drawRequests.size)

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
    override val fRpcStorageApi: FRpcStorageApi get() = error("Not used")
    override val fRpcUpdaterApi: FRpcUpdaterApi get() = error("Not used")
    override val fRpcMatterApi: FRpcMatterApi get() = error("Not used")
    override val fRpcTimeZoneApi: FRpcTimeZoneApi get() = error("Not used")
    override val fRpcBusyApi: FRpcBusyApi get() = error("Not used")
}
