# `LogPlatform.desktop.kt` writes JVM logs to `System.out` / `System.err` without timestamps and uses fragile printing

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/log/src/jvmMain/kotlin/net/flipper/core/busylib/log/LogPlatform.desktop.kt`

## Summary
The desktop JVM actuals print directly to `System.out` / `System.err` without any timestamp, level prefix, or thread name:

```kotlin
actual inline fun info(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        println(logMessage())
    } else {
        println("[$tag] ${logMessage()}")
    }
}
```

Verbose / debug / warn / wtf all go to `System.out`; only `error` goes to `System.err`. There is no level marker on the line, so logs from a server JVM consumer cannot be filtered by severity post-hoc. `error(throwable, …)` calls `error.printStackTrace()` to `stderr` separately from the message, which can interleave with concurrent calls (different threads) producing scrambled output.

## Repro
1. Run with multi-threaded code logging at `info` and `error` simultaneously.
2. Observe interleaved output without level prefixes; cannot distinguish log levels.

## Root Cause
- Quick-and-dirty desktop logger that bypasses any structured logger framework.
- `printStackTrace()` is not synchronised with the println of the message — separate `System.err.println` calls can interleave.

## Impact
- Difficult to debug desktop builds.
- AGENTS.md mandates "Logging: implement LogTagProvider + use TaggedLogger(TAG), not println / Log.d." This implementation literally uses `println`. While that's by design for desktop, it's still substandard.

## Suggested Fix
Use a real logger (`slf4j-simple`, `kotlin-logging`) on JVM, or at minimum:

```kotlin
private fun log(level: String, tag: String?, message: String) {
    val time = Instant.now()
    val thread = Thread.currentThread().name
    val out = if (level == "E") System.err else System.out
    synchronized(out) {
        out.println("$time $level [$thread]${tag?.let { " [$it]" } ?: ""} $message")
    }
}
```
This adds synchronization, level prefix, timestamp, and thread name.
