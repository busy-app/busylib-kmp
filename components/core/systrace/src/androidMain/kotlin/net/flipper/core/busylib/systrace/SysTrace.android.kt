package net.flipper.core.busylib.systrace

import android.os.Trace

actual inline fun beginTraceSection(label: String) {
    Trace.beginSection(label)
}

actual inline fun endTraceSection() {
    Trace.endSection()
}
