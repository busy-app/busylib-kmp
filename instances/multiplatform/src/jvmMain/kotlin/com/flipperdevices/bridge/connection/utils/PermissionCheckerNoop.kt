package com.flipperdevices.bridge.connection.utils

import com.flipperdevices.bridge.connection.screens.utils.PermissionChecker
import com.flipperdevices.core.di.AppGraph
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesBinding(AppGraph::class, binding<PermissionChecker>())
class PermissionCheckerNoop : PermissionChecker {
    override fun isPermissionGranted() = true
}
