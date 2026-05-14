package net.flipper.core.busylib.systrace

expect inline fun beginTraceSection(label: String)

expect inline fun endTraceSection()

inline fun <T> trace(label: String, block: () -> T): T {
    beginTraceSection(label)
    try {
        return block()
    } finally {
        endTraceSection()
    }
}
