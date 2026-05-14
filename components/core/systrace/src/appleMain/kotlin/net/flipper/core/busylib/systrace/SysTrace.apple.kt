package net.flipper.core.busylib.systrace

actual inline fun beginTraceSection(label: String) {
    TraceRegistry.tracer?.begin(label)
}

actual inline fun endTraceSection() {
    TraceRegistry.tracer?.end()
}
