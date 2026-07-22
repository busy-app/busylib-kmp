package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.Deferred

fun <T> Deferred<T>.getOrNull(): T? {
    return if (isCompleted) {
        this.getCompleted()
    } else {
        null
    }
}
