package net.flipper.bsb.cloud.rest.utils

import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.transform

suspend fun <T> BUSYLibUserPrincipal.Token.run(
    dispatcher: CoroutineDispatcher,
    block: suspend BsbUserPrincipalScopeImpl.() -> T
): Result<T> {
    return withContext(dispatcher) {
        runSuspendCatching {
            getToken(null)
        }.transform { originalToken ->
            runSuspendCatching {
                with(BsbUserPrincipalScopeImpl(originalToken)) {
                    block()
                }
            }.recoverCatching { error ->
                if (error.isAuthError()) {
                    val newToken = getToken(failedToken = originalToken)
                    with(BsbUserPrincipalScopeImpl(newToken)) {
                        block()
                    }
                } else {
                    throw error
                }
            }
        }
    }
}

private fun Throwable.isAuthError(): Boolean {
    if (this !is ResponseException) return false
    val status = response.status
    return status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden
}

class BsbUserPrincipalScopeImpl(val token: String) {
    fun HttpRequestBuilder.addAuth() {
        headers[HttpHeaders.Authorization] = "Bearer $token"
    }
}
