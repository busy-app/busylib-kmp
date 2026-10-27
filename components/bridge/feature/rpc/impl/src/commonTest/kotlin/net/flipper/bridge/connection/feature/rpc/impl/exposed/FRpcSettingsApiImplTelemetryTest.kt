@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.api.model.TelemetryInfo
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class FRpcSettingsApiImplTelemetryTest {
    private class PassThroughObjectCache : ObjectCache {
        override suspend fun <T : Any> getOrElse(
            ignoreCache: Boolean,
            clazz: KClass<T>,
            block: suspend () -> T
        ): Deferred<T> = CompletableDeferred(block())

        override suspend fun clear() = Unit
    }

    private fun TestScope.apiRespondingWith(
        body: String,
        onRequest: (HttpRequestData) -> Unit
    ): FRpcSettingsApiImpl {
        val httpClient: HttpClient = getHttpClient(
            MockEngine { request ->
                onRequest(request)
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
        )
        return FRpcSettingsApiImpl(
            httpClient = httpClient,
            dispatcher = StandardTestDispatcher(testScheduler),
            objectCache = PassThroughObjectCache()
        )
    }

    @Test
    fun GIVEN_telemetry_on_bar_WHEN_read_THEN_gets_consent_from_telemetry_endpoint() = runTest {
        var method: HttpMethod? = null
        var path: String? = null
        val api = apiRespondingWith(
            body = """{"enabled":true,"asked":false}""",
            onRequest = { request ->
                method = request.method
                path = request.url.encodedPath
            }
        )

        val result = api.getTelemetry(ignoreCache = true)

        assertEquals(HttpMethod.Get, method)
        assertEquals("/api/telemetry", path)
        assertEquals(TelemetryInfo(enabled = true, asked = false), result.getOrNull())
    }

    @Test
    fun GIVEN_consent_answer_WHEN_saved_THEN_puts_consent_to_telemetry_endpoint() = runTest {
        var method: HttpMethod? = null
        var path: String? = null
        var body: String? = null
        val api = apiRespondingWith(
            body = """{"result":"ok"}""",
            onRequest = { request ->
                method = request.method
                path = request.url.encodedPath
                body = (request.body as TextContent).text
            }
        )

        val result = api.setTelemetry(TelemetryInfo(enabled = false, asked = true))

        assertEquals(HttpMethod.Put, method)
        assertEquals("/api/telemetry", path)
        assertEquals(
            TelemetryInfo(enabled = false, asked = true),
            Json.decodeFromString(TelemetryInfo.serializer(), body.orEmpty())
        )
        assertEquals("ok", result.getOrNull()?.result)
    }
}
