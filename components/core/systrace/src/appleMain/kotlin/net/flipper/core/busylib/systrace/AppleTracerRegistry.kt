package net.flipper.core.busylib.systrace

object AppleTracerRegistry {
    var tracer: NativeTracer = NoOpTracer
}
