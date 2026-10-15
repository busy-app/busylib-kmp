package net.flipper.tools.debug.api.api

import net.flipper.busylib.core.wrapper.CResult

interface DebugToolsApi {
    suspend fun resetSetupFinishedBefore(): CResult<Unit>
}
