package net.flipper.tools.oncall.api

import net.flipper.bridge.connection.feature.oncall.api.model.OnCallAnimation

interface OnCallSingletonApi {
    fun start(animation: OnCallAnimation)
    fun stop()
}
