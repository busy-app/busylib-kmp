package net.flipper.bridge.connection.utils

import net.flipper.bridge.connection.screens.utils.PermissionChecker

class PermissionCheckerNoop : PermissionChecker {
    override fun isPermissionGranted() = true
}
