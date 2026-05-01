# `runCatching` inside `suspend` paths violates project rules and swallows cancellation

## Type
infrastructure

**Severity:** medium

**Files:**
- `components/bridge/feature/screen-streaming/impl/src/commonMain/kotlin/net/flipper/bridge/connection/feature/screenstreaming/impl/delegates/ScreenFramesProvider.kt` (line 20)
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/BBConfigSettingsKrate.kt` (lines 31–33)

## Summary

`AGENTS.md` explicitly forbids `runCatching` inside `suspend` code, requiring `runSuspendCatching`. Two of
the audited files break this rule, and both end up swallowing `CancellationException`:

1. `ScreenFramesProvider.getScreens()`:
   ```kotlin
   .mapNotNull { runCatching { mapFrame(it) }.getOrNull() }
   ```
   `mapFrame` is not `suspend` today, so `runCatching` here is technically not inside a suspending
   computation, but the lambda runs *inside* a flow operator chain that *is* suspending — if the flow is
   cancelled mid-frame, the `CancellationException` is silently swallowed by `runCatching`, and the outer
   `mapNotNull` keeps emitting `null`s instead of propagating cancellation. The flow keeps running until the
   next upstream emission triggers another check.

2. `BBConfigSettingsKrateImpl.loader`:
   ```kotlin
   .map { stringValue ->
       ...
       runCatching { json.decodeFromString(Serializer, stringValue) }
           .getOrNull()
           ?: Factory.create()
   }
   ```
   Same hazard: the `loader` lambda is consumed inside a Flow, and a cancellation during deserialization is
   silently absorbed.

## Repro

1. Subscribe to `screenFlow.getScreens()`. Cancel the collecting coroutine while a heavy frame is being
   mapped. Observe that cancellation propagation is delayed by one upstream emission.
2. Same pattern for the config loader.

## Root cause

`kotlin.runCatching { }` catches *all* `Throwable`s, including `CancellationException`. The library has
`net.flipper.core.busylib.ktx.common.runSuspendCatching` precisely to avoid this footgun.

## Impact

- Cancellation is delayed. In screen-streaming this means dangling frame decoders and a brief wasted CPU
  burst after the consumer disconnects.
- Violates `AGENTS.md` rule (could be a detekt finding once the rule is exhaustive).

## Suggested fix

Use `runSuspendCatching` in both places, or do the catch with explicit `try/catch` that re-throws
`CancellationException`:

```kotlin
.mapNotNull { frame ->
    try { mapFrame(frame) } catch (c: CancellationException) { throw c } catch (t: Throwable) {
        error(t) { "decode frame failed" }
        null
    }
}
```
