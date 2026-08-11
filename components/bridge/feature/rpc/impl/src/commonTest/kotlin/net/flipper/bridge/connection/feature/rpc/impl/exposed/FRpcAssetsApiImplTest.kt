package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.rpc.api.exception.DrawLowPriorityException
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val LOW_PRIORITY_BODY = """{"error":"Not drawn due to low priority"}"""

class FRpcAssetsApiImplTest {

    private fun TestScope.apiRespondingWith(
        status: HttpStatusCode,
        body: String
    ): FRpcAssetsApiImpl {
        val httpClient: HttpClient = getHttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
        )
        return FRpcAssetsApiImpl(
            httpClient = httpClient,
            dispatcher = StandardTestDispatcher(testScheduler)
        )
    }

    private fun drawRequest() = DrawRequest(
        appId = "draw_tool",
        priority = 40,
        elements = listOf(
            DrawRequest.Element(
                id = "draw_tool",
                timeoutSec = 0,
                type = DrawRequest.Element.ElementType.IMAGE,
                path = "temp.png"
            )
        )
    )

    @Test
    fun GIVEN_low_priority_rejection_WHEN_displayDraw_THEN_fails_with_DrawLowPriorityException() =
        runTest {
            val api = apiRespondingWith(HttpStatusCode.Conflict, LOW_PRIORITY_BODY)

            val result = api.displayDraw(drawRequest())

            assertIs<DrawLowPriorityException>(result.exceptionOrNull())
        }

    @Test
    fun GIVEN_low_priority_rejection_on_200_WHEN_displayDraw_THEN_still_maps_to_exception() =
        runTest {
            // The status code is not what identifies the case — the error payload is.
            val api = apiRespondingWith(HttpStatusCode.OK, LOW_PRIORITY_BODY)

            val result = api.displayDraw(drawRequest())

            assertIs<DrawLowPriorityException>(result.exceptionOrNull())
        }

    @Test
    fun GIVEN_successful_draw_WHEN_displayDraw_THEN_returns_success_response() = runTest {
        val api = apiRespondingWith(HttpStatusCode.OK, """{"result":"ok"}""")

        val result = api.displayDraw(drawRequest())

        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull()?.result)
    }

    @Test
    fun GIVEN_unrelated_error_WHEN_displayDraw_THEN_fails_without_low_priority_exception() =
        runTest {
            val api = apiRespondingWith(
                HttpStatusCode.BadRequest,
                """{"error":"Nothing to display"}"""
            )

            val result = api.displayDraw(drawRequest())

            val error = result.exceptionOrNull()
            assertTrue(error !is DrawLowPriorityException)
            assertEquals("Nothing to display", error?.message)
        }

    @Test
    fun GIVEN_low_priority_payload_WHEN_removeDraw_THEN_is_left_alone() = runTest {
        // Only /api/display/draw POST rejects on priority; DELETE has no such contract,
        // so the payload must not be reinterpreted there.
        val api = apiRespondingWith(HttpStatusCode.Conflict, LOW_PRIORITY_BODY)

        val result = api.removeDraw(appId = "draw_tool")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() !is DrawLowPriorityException)
    }
}
