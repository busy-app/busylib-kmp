package com.flipperdevices.busylib.core.network

import androidx.lifecycle.Lifecycle
import com.flipperdevices.core.network.BUSYLibNetworkStateApi

interface BUSYLibAndroidNetworkStateApi : BUSYLibNetworkStateApi {
    /**
     * Registers a lifecycle to be tracked for determining network availability.
     * Network will only be considered available when any of the registered lifecycles
     * are at least in [shouldBeState]. Call this for foreground services to ensure
     * network requests respect Android 15's process lifecycle requirements.
     *
     * @param lifecycle The lifecycle to track (e.g., from a LifecycleService)
     * @param shouldBeState The minimum lifecycle state required for this lifecycle
     *  to be considered active. Defaults to [Lifecycle.State.STARTED].
     */
    fun addLifecycle(lifecycle: Lifecycle, shouldBeState: Lifecycle.State = Lifecycle.State.STARTED)
}
