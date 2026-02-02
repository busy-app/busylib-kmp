package net.flipper.bsb.cloud.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap

interface BUSYLibHostApi {
    fun getHost(): WrappedStateFlow<String>
}

class BUSYLibHostApiStub(val host: String): BUSYLibHostApi {
    override fun getHost() = MutableStateFlow(host).wrap()
}