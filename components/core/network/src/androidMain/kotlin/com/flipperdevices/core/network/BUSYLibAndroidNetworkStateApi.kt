package com.flipperdevices.core.network

import androidx.lifecycle.Lifecycle

interface BUSYLibAndroidNetworkStateApi : BUSYLibNetworkStateApi {
    /**
     * Registers a lifecycle to be tracked for determining network availability.
     * Network will only be considered available when all registered lifecycles
     * are at least in STARTED state. Call this for foreground services to ensure
     * network requests respect Android 15's process lifecycle requirements.
     *
     * @param lifecycle The lifecycle to track (e.g., from a LifecycleService)
     */
    fun addLifecycle(lifecycle: Lifecycle)
}
