# Test code uses `runCatching` to wrap a `suspend` call

## Severity
low

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonTest/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImplTest.kt:1195`

## Summary
The test `GIVEN_parent_scope_cancelled_WHEN_tryUpdateConnectionConfig_THEN_handles_gracefully` wraps a `suspend` call with `runCatching { sut.tryUpdateConnectionConfig(...) }`. Per AGENTS.md:

> No `runCatching` inside `suspend` functions — use `net.flipper.core.busylib.ktx.common.runSuspendCatching` instead.

`runCatching` swallows `CancellationException`, which can mask scope-cancellation propagation in the very test that asserts cancellation is handled gracefully.

## Repro
N/A — code-style / safety rule violation. Detekt's `RunCatchingInSuspendRule` should flag it eventually.

## Root Cause
Habit. The test was written to "make sure it doesn't throw"; `runCatching` was the easy reach.

## Impact
- Hides cancellation propagation if the SUT actually does fail with `CancellationException`.
- Breaks the project's hard rule on `runCatching` use in suspend contexts.

## Suggested Fix
Replace with `runSuspendCatching` from `net.flipper.core.busylib.ktx.common`:

```kotlin
val result = runSuspendCatching { sut.tryUpdateConnectionConfig(config2) }
```
