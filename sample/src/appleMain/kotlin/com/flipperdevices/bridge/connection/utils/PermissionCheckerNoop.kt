package com.flipperdevices.bridge.connection.utils

import com.flipperdevices.bridge.connection.screens.utils.PermissionChecker

class PermissionCheckerNoop : PermissionChecker {
    override fun isPermissionGranted() = true
}
