@file:Suppress("ClockNowForbiddenRule")

package com.flipperdevices.core.ktx.android

import com.flipperdevices.core.buildkonfig.BuildKonfig
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.verbose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

fun <T> LogTagProvider.runBlockingWithLog(
    tag: String? = null,
    block: suspend CoroutineScope.() -> T
): T {
    var startTime: Long = 0
    if (BuildKonfig.IS_LOG_ENABLED) {
        startTime = System.currentTimeMillis()
    }
    return runBlocking {
        verbose { "Waiting time for job $tag is ${System.currentTimeMillis() - startTime}ms" }
        verbose {
            startTime = System.currentTimeMillis()
            "Launch $tag job in blocking mode..."
        }
        val result = block()
        verbose { "Complete $tag job in ${System.currentTimeMillis() - startTime}ms" }
        return@runBlocking result
    }
}
