# high — `FHTTPDeviceApi.getCapabilities()` default returns a single-shot terminating flow

## Severity
high

## Type
broken-feature

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/serial/FHTTPDeviceApi.kt:8-24`

## Summary
The interface `FHTTPDeviceApi` ships a default implementation:
```kotlin
fun getCapabilities(): Flow<List<FHTTPTransportCapability>> = flowOf(emptyList())
```
`flowOf(emptyList())` is a *cold, single-emission* flow that completes immediately after one collect. The doc-comment even acknowledges this is wrong ("Implementations should override this and return a Flow that emits the current list of capabilities instead of just wrapping a static list with `flowOf()`."). Despite the warning, the default is still `flowOf(...)` so any transport that simply forgets to override silently behaves as a terminating flow.

## Reproduction / scenario
1. Add a new transport implementing `FHTTPDeviceApi` and forget to override `getCapabilities()`.
2. The combined transport composes capabilities via:
   ```kotlin
   private val _capabilities = connectionPool.get().map { ... }   // FCombinedConnectionApiImpl.kt:132
   ```
   When that map runs through `combine` / `flatMapLatest` over a flow that completes, the composite stream **terminates** as soon as the inner flow ends. Anything that uses `hasCapability(...)` (`FHTTPDeviceApi.kt:19-24`) downstream then sees the flow complete and stops emitting forever.
3. The collector that drives feature-availability decisions stops re-evaluating; the UI is stuck on "no capabilities".

## Why it happens
`flowOf(...)` is being used where a "current state" (StateFlow) is required. There is no contractual way for the interface to express "must be a hot, never-completing flow", so the default silently violates the implicit contract.

## Impact
- Any partially-implemented transport degrades the combined transport: `hasCapability` returns a finite flow.
- Default implementations encourage forgetting the override, since the code compiles and returns "no capabilities" — appearing benign but breaking downstream `combine` operators.

## Suggested fix
- Either remove the default entirely (force every implementation to provide capabilities), or change the default to a hot type, e.g. return `MutableStateFlow(emptyList())`/`flow { emit(emptyList()); awaitCancellation() }` so collectors don't terminate.
- Better still, change the return type to `WrappedStateFlow<List<FHTTPTransportCapability>>` (also fixes the bare-Flow rule) — the type itself enforces the "hot, current state" contract.
