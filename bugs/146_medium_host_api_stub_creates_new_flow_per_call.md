# `BUSYLibHostApiStub.getHost()` creates a new `MutableStateFlow` on every call

## Severity
medium

## Type
broken-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/api/src/commonMain/kotlin/net/flipper/bsb/cloud/api/BUSYLibHostApi.kt` (lines 11-13)

## Summary
```kotlin
class BUSYLibHostApiStub(val host: String) : BUSYLibHostApi {
    override fun getHost() = MutableStateFlow(host).wrap()
}
```

Each call to `getHost()` allocates a new `MutableStateFlow`, wraps it, and returns
it. Different consumers see different `WrappedStateFlow` instances even though they
all carry the same constant value.

Because the host is constant in this stub, it is functionally OK in production. But:
- Tests/sample code that compare flow identity (`assertSame(api.getHost(), api.getHost())`) will fail.
- Memory churn on each call (new flow + wrapper).
- If anyone in the future adds a "set" path on the stub without re-reading the source,
  they will lose updates because every consumer holds its own private flow.

## Repro
```kotlin
val api = BUSYLibHostApiStub("h")
val a = api.getHost()
val b = api.getHost()
require(a !== b) // distinct instances, easy to misuse
```

## Root Cause
The stub does not memoize the `MutableStateFlow`.

## Impact
- Footgun if someone replaces this stub with a mutable variant; updates are silently
  lost.
- Wasted allocations.

## Suggested Fix
```kotlin
class BUSYLibHostApiStub(val host: String) : BUSYLibHostApi {
    private val flow = MutableStateFlow(host).wrap()
    override fun getHost() = flow
}
```
