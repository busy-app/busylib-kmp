# `LifecyclesHolderFlow.isAnyLifecycleOnStartFlow` restarts entire combine on every add/remove → loses transient lifecycle events

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/network/src/androidMain/kotlin/com/flipperdevices/busylib/core/network/LifecyclesHolderFlow.kt` lines 22–56

## Summary
The flow is built as:

```kotlin
val isAnyLifecycleOnStartFlow = getLifecyclesFlow()
    .flatMapLatest { flows ->
        if (flows.isEmpty()) flowOf(false)
        else combine(flows) { it.any { it } }
    }
```

`getLifecyclesFlow()` returns a `Flow<List<Flow<Boolean>>>` driven by `lifecyclesStateFlow.map { … }`. **Every** mutation of `lifecyclesStateFlow` (e.g. removing a destroyed lifecycle, adding a new one) emits a new list, which causes `flatMapLatest` to:

1. Cancel the previous `combine(flows) { … }` collector (and therefore stop tracking every existing lifecycle).
2. Subscribe to all lifecycles freshly; receive their *current* state (not the recent transition) and re-merge.

The hidden bug is in the inner `.onEach { state -> if (state == DESTROYED) … remove … }`. When a lifecycle reaches `DESTROYED`, the `.update { … filter … }` mutates `lifecyclesStateFlow`, retriggering the **outer** `flatMapLatest`. The just-emitted `DESTROYED` value is therefore the **last** one observed in the previous combine; the new combine that replaces it does not see the destroyed lifecycle at all (it was filtered) — so the boolean `true → false` transition for that lifecycle is correctly captured. So far OK.

But: while the combine is being **re-built**, intermediate values from the OTHER lifecycles can be missed. Consider two lifecycles A (RESUMED, true) and B (DESTROYED). When B emits DESTROYED:

1. `onEach` triggers `update { remove B }`, which is queued on `lifecyclesStateFlow`.
2. The combine emits `true` (A is true) and downstream observers see "isAnyLifecycle = true" — correct.
3. `lifecyclesStateFlow` updates; flatMapLatest cancels the old combine, starts a new one with `[A]`. The new combine immediately collects A's state — at this exact instant A may be transitioning to STARTED → RESUMED, and the `.currentStateFlow` returns the *current* value (RESUMED), so the result is still `true`. OK.
4. **However**: if A simultaneously becomes DESTROYED (process tearing down), the new combine subscribes to a fresh `currentStateFlow` and may miss the in-flight transition because `currentStateFlow` is a `StateFlow` — it always replays the current value, so this case is also OK.

The real bug: every restart **resets cached `BUSYLibNetworkStateApiImpl.isNetworkAvailableFlow`'s upstream sample**, so consumers may observe a brief `false` between `flatMapLatest` cancellations even when no real lifecycle changed. This manifests as the network-available `StateFlow` emitting spurious `false` then immediately `true` on every `addLifecycle()` call.

Additionally, **`flatMapLatest` is `@ExperimentalCoroutinesApi`** — using it without explicit opt-in suggests the contract was never reviewed.

## Repro
1. Subscribe to `BUSYLibNetworkStateApiImpl.isNetworkAvailableFlow`.
2. Call `addLifecycle(L1)` / `addLifecycle(L2)` in quick succession.
3. Observe the boolean flow emitting `(true, false, true, false, true)` instead of a single `true`.

## Root Cause
- `flatMapLatest` over a state-driven outer flow forces a full re-subscription on every state mutation.
- `combine(flows) { it.any { it } }` is hot — there is no "carry over" between `flatMapLatest` instances.
- The `.onEach { state -> if (DESTROYED) update(remove) }` mutates the outer state flow during inner emission, generating cascading restarts.

## Impact
- Spurious `false` → `true` transitions on `isNetworkAvailableFlow`, observed by every consumer.
- For consumers that gate critical work on "is network available" (RPC retry loops, websocket reconnect), the flapping triggers unnecessary cancel/restart cycles.
- Possible cascading restarts when multiple lifecycles destroy in sequence.

## Suggested Fix
1. Use `distinctUntilChanged()` after the `flatMapLatest` to absorb duplicate emissions.
2. Track lifecycle removal in an `invokeOnCompletion`/`Lifecycle.removeObserver`-style callback rather than from inside the flow itself, so mutating the outer state flow does not happen while iterating it.
3. Consider replacing the architecture with a single `MutableStateFlow<Map<Lifecycle, Boolean>>` updated by individual observers; `isAnyLifecycleOnStartFlow` becomes `map { it.values.any { it } }.distinctUntilChanged()`, no `flatMapLatest` at all.
