package net.flipper.core.busylib.systrace

interface NativeTracer {
    fun begin(name: String)
    fun end()

    companion object : NativeTracer {
        private val tracer: NativeTracer
            get() = NativeTracer()

        override fun begin(name: String) {
            tracer.begin(name)
        }

        override fun end() {
            tracer.end()
        }
    }
}

inline fun <T> NativeTracer.trace(label: String, block: () -> T): T {
    begin(label)
    try {
        return block()
    } finally {
        end()
    }
}
