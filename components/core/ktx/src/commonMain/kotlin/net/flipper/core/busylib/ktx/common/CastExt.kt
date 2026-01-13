package net.flipper.core.busylib.ktx.common

inline fun <reified T> Any.tryCast() = this as? T
