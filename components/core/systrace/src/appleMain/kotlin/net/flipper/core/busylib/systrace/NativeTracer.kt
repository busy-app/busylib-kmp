package net.flipper.core.busylib.systrace

interface NativeTracer {
    fun begin(name: String)
    fun end()
}
