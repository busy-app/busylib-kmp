package net.flipper.core.busylib.systrace

actual fun NativeTracer(): NativeTracer {
    return NoOpTracer
}
