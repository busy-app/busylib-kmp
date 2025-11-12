package net.flipper.core.busylib.ktx.common

import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal actual fun getDispatcher(): CoroutineDispatcher {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Executors.newWorkStealingPool().asCoroutineDispatcher()
    } else {
        Dispatchers.Default
    }
}
