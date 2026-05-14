package net.flipper.core.busylib.systrace

import android.os.Trace

class AndroidNativeTracer : NativeTracer {
    override fun begin(name: String) {
        Trace.beginSection(name)
    }

    override fun end() {
        Trace.endSection()
    }
}
