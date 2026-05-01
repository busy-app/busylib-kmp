# `DefaultAppleLogger` only escapes `%` once — does not protect against tag-side format-string injection

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/log/src/appleMain/kotlin/net/flipper/core/busylib/log/DefaultAppleLogger.kt` lines 7, 9–14, etc.

## Summary
`DefaultAppleLogger` correctly escapes `%` in the message before passing to `NSLog`:

```kotlin
private fun String.escapeForNSLog(): String = replace("%", "%%")

override fun error(tag: String?, message: String) {
    if (tag == null) {
        NSLog(message.escapeForNSLog())
    } else {
        NSLog("[$tag] $message".escapeForNSLog())   // tag concatenated BEFORE escaping — OK
    }
}
```

Note that the tag is concatenated into the same string that is then escaped, so a tag containing `%s` is escaped along with the message. Good.

The actual issue is `NSLog` treats its first argument as a *format string*. Even with `%%` escaping, NSLog's format-parsing also treats `%n` and `%hn` (write-to-pointer) as syntactically valid. Since `replace("%", "%%")` doubles every `%`, `%n` becomes `%%n` (a literal "%n"). So the escape is sufficient for safety against format-string injection at the message level.

But: if `AppleLoggerDelegate.setup(customLogger)` replaces the default with a custom implementation, that implementation has **no** safety contract. Custom loggers can re-introduce the format-string issue. This is a weak invariant for a multi-platform SDK.

## Repro
A consumer-supplied `AppleLogger` that implements `error` as `NSLog(message)` (without escaping) will crash on user-controlled `%s` messages.

## Root Cause
- `AppleLogger` is a public extension point; the contract that custom implementations must escape `%` is not documented.
- No tests assert the safety property.

## Impact
- Crash / undefined behavior in misbehaving consumer-supplied logger.
- Consumer apps installing a custom logger may inadvertently introduce a security issue.

## Suggested Fix
1. Document the requirement on `AppleLogger`'s KDoc that implementations must treat `message` as a literal (i.e., not pass it as a format string to `NSLog`/`os_log`).
2. Consider making the `escapeForNSLog` step part of the public contract — e.g., wrap the delegate so consumer implementations always receive a pre-escaped string.
