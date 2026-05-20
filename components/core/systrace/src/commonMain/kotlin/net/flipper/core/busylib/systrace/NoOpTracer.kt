package net.flipper.core.busylib.systrace

object NoOpTracer : NativeTracer {
    override fun begin(name: String) = Unit
    override fun end() = Unit
}
