package net.flipper.bridge.connection.feature.rpc.impl.critical

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.api.model.BsbRpcException
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCode
import net.flipper.bridge.connection.feature.rpc.api.model.BusyBarLinkCodeAlreadyLinked
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FRpcCriticalFeatureApiImplTest {

    @Test
    fun GIVEN_link_code_response_WHEN_getLinkCode_THEN_returns_code() = runTest {
        val api = api("""{"code":"123456"}""")

        val result = api.getLinkCode()

        assertEquals(BusyBarLinkCode("123456"), result.getOrNull())
    }

    @Test
    fun GIVEN_already_linked_response_WHEN_getLinkCode_THEN_returns_already_linked() = runTest {
        val api = api("""{"error":"Already linked"}""")

        val result = api.getLinkCode()

        assertEquals(BusyBarLinkCodeAlreadyLinked, result.getOrNull())
    }

    @Test
    fun GIVEN_not_connected_response_WHEN_getLinkCode_THEN_fails_with_typed_exception() = runTest {
        val api = api("""{"error":"Not connected"}""")

        val result = api.getLinkCode()

        assertTrue(result.isFailure)
        val exception = assertIs<BsbRpcException>(result.exceptionOrNull())
        assertEquals("Not connected", exception.error)
    }

    @Test
    fun GIVEN_arbitrary_device_error_WHEN_getLinkCode_THEN_fails_with_typed_exception() = runTest {
        val api = api("""{"error":"Some other device error"}""")

        val result = api.getLinkCode()

        assertTrue(result.isFailure)
        val exception = assertIs<BsbRpcException>(result.exceptionOrNull())
        assertEquals("Some other device error", exception.error)
    }

    @Test
    fun GIVEN_unparseable_response_WHEN_getLinkCode_THEN_fails_but_not_typed_exception() = runTest {
        val api = api("""{"unexpected":"payload"}""")

        val result = api.getLinkCode()

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is BsbRpcException)
        assertIs<JsonConvertException>(result.exceptionOrNull())
    }

    private fun api(responseBody: String): FRpcCriticalFeatureApiImpl {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = responseBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                )
            }
        ) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                )
            }
        }
        return FRpcCriticalFeatureApiImpl(client)
    }
}
