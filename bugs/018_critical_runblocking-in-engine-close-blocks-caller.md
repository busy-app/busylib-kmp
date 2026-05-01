# critical — `runBlocking` inside `HttpClientEngine.close()` can deadlock & violates project rules

## Severity
critical

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/engine/BUSYCloudHttpEngine.kt:80-94`
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/engine/BUSYBarHttpEngine.kt:42-56`

## Summary
Both `BUSYBarHttpEngine.close()` and `BUSYCloudHttpEngine.close()` use `kotlinx.coroutines.runBlocking { rwMutex.withWriteLock { … } }` to acquire the `ReadWriteMutex` write lock and tear down the underlying `delegate` engine. AGENTS.md explicitly forbids `runBlocking` in coroutine code. More importantly, this is invoked from `FLanApiImpl.disconnect()` / `FCloudApiImpl.disconnect()` — both already `suspend` functions — so blocking the caller's thread is never necessary and is positively harmful.

## Repro
1. Caller is a Dispatchers.Main / single-thread dispatcher (typical for iOS UI).
2. There is an in-flight `execute(...)` call holding the read lock; that call has already suspended at `delegate.execute(newRequestData)` and is waiting on a network reply.
3. UI code calls `disconnect()` on the same dispatcher.
4. `runBlocking` parks the dispatcher; the in-flight request is still suspended on it; the thread cannot resume the read-lock holder, so `withWriteLock` never gets the lock; the dispatcher is dead.

Even on a multi-threaded dispatcher, `runBlocking` on iOS pins a worker, ignores upstream cancellation (calling `disconnect()` from a coroutine that gets cancelled while `close()` is blocked will leak the thread until the request finally returns), and silently swallows `CancellationException`.

## Root Cause
`HttpClientEngineBase.close()` is non-suspending, but the desired teardown ("wait for in-flight requests, then close delegate") is naturally suspending. The implementer reached for `runBlocking` instead of moving the suspending part up to the `disconnect()` boundary.

## Impact
- Deadlock on single-threaded dispatchers when a request is in-flight.
- Cancellation of `disconnect()` does NOT cancel the underlying network call — the thread is wedged until the OS times out.
- Violates project rule "no runBlocking" (AGENTS.md) and SKIE Swift interop expectations (Swift bridges to suspend; runBlocking is invisible to the bridge).
- Same pattern duplicated in two engines, both production paths.

## Suggested Fix
Make the pre-cleanup suspending and call it from `disconnect()` before `close()`:

```kotlin
suspend fun shutdown() {
    rwMutex.withWriteLock {
        if (closed) return@withWriteLock
        closed = true
        delegate.close()
    }
}

override fun close() {
    // best-effort, no blocking; assumes shutdown() already ran
    if (!closed) delegate.close()
    super.close()
}
```

Then `FLanApiImpl.disconnect()` calls `httpEngine.shutdown()` first, then `httpEngine.close()`. Same for cloud. Drop `runBlocking` entirely.
