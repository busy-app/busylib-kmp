@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.rpc.impl.exposed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.feature.rpc.api.exception.DeviceNameRejectedException
import net.flipper.bridge.connection.feature.rpc.api.model.NameInfo
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.core.busylib.ktx.common.cache.ObjectCache
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val DISALLOWED_CHARACTER_BODY = """{"error":"Name contains disallowed character"}"""

class FRpcSettingsApiImplTest {

    private class PassThroughObjectCache : ObjectCache {
        override suspend fun <T : Any> getOrElse(
            ignoreCache: Boolean,
            clazz: KClass<T>,
            block: suspend () -> T
        ): Deferred<T> = CompletableDeferred(block())

        override suspend fun clear() = Unit
    }

    private fun TestScope.apiRespondingWith(
        status: HttpStatusCode,
        body: String
    ): FRpcSettingsApiImpl {
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
        return FRpcSettingsApiImpl(
            httpClient = httpClient,
            dispatcher = StandardTestDispatcher(testScheduler),
            objectCache = PassThroughObjectCache()
        )
    }

    @Test
    fun GIVEN_disallowed_character_rejection_WHEN_setName_THEN_fails_with_bar_reason() = runTest {
        val api = apiRespondingWith(HttpStatusCode.BadRequest, DISALLOWED_CHARACTER_BODY)

        val result = api.setName(NameInfo("Бизи бар"))

        val error = assertIs<DeviceNameRejectedException>(result.exceptionOrNull())
        assertEquals("Name contains disallowed character", error.reason)
    }

    @Test
    fun GIVEN_unknown_rejection_WHEN_setName_THEN_reason_is_passed_through() = runTest {
        val api = apiRespondingWith(HttpStatusCode.BadRequest, """{"error":"Storage is full"}""")

        val result = api.setName(NameInfo("BUSY bar"))

        val error = assertIs<DeviceNameRejectedException>(result.exceptionOrNull())
        assertEquals("Storage is full", error.reason)
    }

    @Test
    fun GIVEN_accepted_name_WHEN_setName_THEN_returns_success_response() = runTest {
        val api = apiRespondingWith(HttpStatusCode.OK, """{"result":"ok"}""")

        val result = api.setName(NameInfo("BUSY bar"))

        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull()?.result)
    }

    @Test
    fun GIVEN_malformed_body_WHEN_setName_THEN_fails_without_rejection_exception() = runTest {
        val api = apiRespondingWith(HttpStatusCode.InternalServerError, "not json")

        val result = api.setName(NameInfo("BUSY bar"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() !is DeviceNameRejectedException)
    }
}
