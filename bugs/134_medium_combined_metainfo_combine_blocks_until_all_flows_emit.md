# `CombinedMetaInfoApiImpl.get` blocks emission until **all** transports have emitted at least once

## Severity
medium

## Type
broken-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/metakey/CombinedMetaInfoApiImpl.kt:42-47`

## Summary
`combine(flows) { results -> results.find { it.isSuccess } ?: failure }` only emits once **every** flow has produced its first value. If one transport's meta-info flow is slow (e.g. waiting for a Cloud round-trip) and another transport already has the answer locally, the consumer is blocked behind the slowest transport.

## Repro
1. Combined config: BLE (instant local cache) + Cloud (slow round-trip).
2. Subscribe to `metaInfoApi.get(DEVICE_NAME)`.
3. BLE has the data immediately. Cloud takes 5 s. The consumer waits 5 s for the first emission, even though BLE already has the answer.

## Root Cause
Standard `kotlinx.coroutines.flow.combine` semantics: it emits when **all** sources have at least one value. The combine here doesn't account for the "first-success-wins" intent.

## Impact
- Latency dominated by the slowest transport, not the fastest.
- Defeats one of the main reasons to have a combined transport.

## Suggested Fix
Replace `combine` with a custom merge that emits as soon as any flow reports `Result.success`, falling back to a failure only when **all** flows have reported failure or completed. Sketch:

```kotlin
return delegates.flatMapLatest { currentDelegates ->
    if (currentDelegates.isEmpty()) flowOf(Result.failure(NoSuchElementException(...)))
    else channelFlow {
        val flows = currentDelegates.map { it.get(key) }
        // emit a value whenever a flow updates; aggregate by "any success wins"
        val states = arrayOfNulls<Result<Flow<TransportMetaInfoData?>>>(flows.size)
        flows.forEachIndexed { i, f ->
            launch {
                f.collect { r ->
                    states[i] = r
                    val winner = states.firstNotNullOfOrNull { it?.takeIf { res -> res.isSuccess } }
                        ?: states.firstOrNull { it != null }
                        ?: Result.failure(NoSuchElementException("No transport supports key $key"))
                    send(winner)
                }
            }
        }
    }.distinctUntilChanged()
}
```

Bonus: respect transport priority for tiebreaks (see related bug `high_combined_meta_info_uses_undefined_transport_order`).
