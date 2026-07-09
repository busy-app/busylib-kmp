package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import kotlin.time.Duration.Companion.seconds

class OnCallDisplayLoop(
    private val rpcAssetsApi: FRpcAssetsApi
) : LogTagProvider {
    override val TAG: String = "OnCallDisplayLoop"

    private fun createDrawRequest(): DrawRequest {
        return DrawRequest(
            appId = APP_ID,
            priority = DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = ANIM_ID,
                    display = DrawRequest.Display.FRONT,
                    type = DrawRequest.Element.ElementType.ANIMATION,
                    stockPath = ANIM_PATH,
                    loop = true,
                    timeoutSec = DISPLAY_TIMEOUT.inWholeSeconds.toInt()
                )
            )
        )
    }

    private suspend fun performStopAttempt(): Result<Unit> {
        return rpcAssetsApi.removeDraw(appId = APP_ID).map { }
    }

    suspend fun run() {
        try {
            while (currentCoroutineContext().isActive) {
                rpcAssetsApi
                    .displayDraw(createDrawRequest())
                    .onFailure { t -> error(t) { "Failed to display draw" } }
                delay(UPDATE_DELAY)
            }
        } finally {
            withContext(NonCancellable) {
                withTimeoutOrNull(STOP_TIMEOUT) {
                    performStopAttempt()
                }
            }
        }
    }

    companion object {
        private const val APP_ID = "busy_lib_on_call"
        private const val DRAW_PRIORITY = 50
        private const val ANIM_ID = "busy_lib_on_call_anim"
        private const val ANIM_PATH = "shared/on_call_72x16.anim"
        private val DISPLAY_TIMEOUT = 30.seconds
        private val UPDATE_DELAY = 10.seconds
        private val STOP_TIMEOUT = 3.seconds
    }
}
