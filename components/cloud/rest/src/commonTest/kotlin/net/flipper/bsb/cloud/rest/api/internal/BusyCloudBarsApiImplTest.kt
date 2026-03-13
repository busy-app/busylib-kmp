package net.flipper.bsb.cloud.rest.api.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.api.BUSYLibHostApiStub
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class BusyCloudBarsApiImplTest {
    @Test
    fun GIVEN_unlinkBusyBar_WHEN_called_THEN_request_is_sent_to_expected_url() = runTest {
        val host = "test.flipperzero.one"
        val principal = BUSYLibUserPrincipal.Token("test-token")
        val uuid = Uuid.parse("019588ec-6e11-7f56-b24a-bb74d2fb0d5f")

        var requestMethod: HttpMethod? = null
        var requestUrl: String? = null
        var authHeader: String? = null

        val httpClient = HttpClient(
            MockEngine { request ->
                requestMethod = request.method
                requestUrl = request.url.toString()
                authHeader = request.headers[HttpHeaders.Authorization]

                respond(
                    content = "",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Text.Plain.toString()
                    )
                )
            }
        )

        val api = BusyCloudBarsApiImpl(
            httpClient = httpClient,
            dispatcher = StandardTestDispatcher(testScheduler),
            bsbHostApi = BUSYLibHostApiStub(host)
        )

        val result = api.unlinkBusyBar(principal, uuid)

        assertTrue(result.isSuccess)
        assertEquals(HttpMethod.Delete, requestMethod)
        assertEquals("https://$host/api/v0/bars/$uuid", requestUrl)
        assertEquals("Bearer ${principal.token}", authHeader)
    }
}
