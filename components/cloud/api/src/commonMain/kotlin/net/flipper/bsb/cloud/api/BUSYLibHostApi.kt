package net.flipper.bsb.cloud.api

import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

interface BUSYLibHostApi {
    fun getHost(): WrappedStateFlow<String>
    fun getProxyHost(): WrappedStateFlow<String>
}

class BUSYLibHostApiStub(
    val host: String,
    val proxyHost: String
) : BUSYLibHostApi {
    override fun getHost() = MutableStateFlow(host).wrap()

    override fun getProxyHost() = MutableStateFlow(proxyHost).wrap()
}
