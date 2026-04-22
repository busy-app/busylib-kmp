package net.flipper.bsb.watchers.provisioning.fakes

import com.flipperdevices.core.network.BUSYLibNetworkStateApi
import kotlinx.coroutines.flow.MutableStateFlow
import net.flipper.busylib.core.wrapper.wrap

internal class FakeNetworkStateApi(
    flow: MutableStateFlow<Boolean>
) : BUSYLibNetworkStateApi {
    override val isNetworkAvailableFlow = flow.wrap()
}
