package com.flipperdevices.bridge.connection.utils

import com.flipperdevices.bridge.connection.screens.utils.PermissionChecker
import com.flipperdevices.busylib.core.di.BusyLibGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesBinding(BusyLibGraph::class, binding<PermissionChecker>())
class PermissionCheckerNoop : PermissionChecker {
    override fun isPermissionGranted() = true
}