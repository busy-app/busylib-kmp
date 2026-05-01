# `getHttpClient()` (parameterless) creates a fresh OkHttp/Darwin engine on every invocation

## Type
infrastructure

**Severity:** medium

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/HttpClient.kt` lines 27–31
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/androidMain/kotlin/net/flipper/core/ktor/HttpEnginePlatform.android.kt` lines 9–20
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/jvmMain/kotlin/net/flipper/core/ktor/HttpEnginePlatform.jvm.kt` lines 10–22

## Summary
`fun getHttpClient() = getHttpClient(getPlatformEngineFactory())` calls the engine factory's `create()` on every invocation. On Android and JVM, this constructs a fresh `OkHttpClient.Builder().pingInterval(...).build()` — each `OkHttpClient` owns its own connection pool, dispatcher, thread pools, and DNS/cache. While `KtorModule` correctly scopes `getHttpClient()` to `@SingleIn(BusyLibGraph::class)` (so the DI graph holds only one), **any direct caller** of `getHttpClient()` outside DI gets a brand-new engine per call.

```kotlin
fun getHttpClient() = getHttpClient(getPlatformEngineFactory())   // creates new engine

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> =
    object : HttpClientEngineFactory<OkHttpConfig> {
        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine =
            OkHttp.create {
                block()
                preconfigured = OkHttpClient.Builder().pingInterval(WS_PING_INTERVAL).build()
            }
    }
```

`OkHttp.create { preconfigured = OkHttpClient.Builder()...build() }` constructs a new `OkHttpClient`. Each `OkHttpClient` defaults to ~5 dispatcher threads + connection pool; not closing it leaks them.

Additionally, the resulting `HttpClient` is closeable but no caller's lifecycle ties to it — neither `KtorModule` nor any consumer ever calls `client.close()`. Daemon threads are released only on JVM exit.

## Repro
```kotlin
repeat(100) { getHttpClient() }
// 100 OkHttpClient instances, ~500 idle dispatcher threads
```

## Root Cause
- The `HttpClientEngineFactory` is constructed inline as an anonymous object that *itself* allocates a fresh `OkHttpClient`. There is no lazy/cached singleton.
- `KtorModule` is the only caller that scopes via `@SingleIn`; any future test code or sample code that calls `getHttpClient()` directly bypasses that.
- No `HttpClient.close()` is ever called.

## Impact
- Engine leak when `getHttpClient()` is called outside DI (test fixtures, debug utilities).
- Thread leak on JVM/Android: idle OkHttp dispatcher threads survive scope cancellation.
- HTTP/2 connection-pool fragmentation: every engine has its own pool, so concurrent requests don't share connections.

## Suggested Fix
1. Cache the engine factory:
   ```kotlin
   private val sharedEngineFactory: HttpClientEngineFactory<OkHttpConfig> by lazy { … }
   actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> = sharedEngineFactory
   ```
2. Cache the `OkHttpClient` itself inside the factory:
   ```kotlin
   private val sharedOkHttp: OkHttpClient by lazy {
       OkHttpClient.Builder().pingInterval(WS_PING_INTERVAL).build()
   }
   ```
3. Document `getHttpClient()` as creating a new client owned by the caller; rename to `createHttpClient()`.
4. Hook `client.close()` into the DI module via `@OnDestroy` or equivalent.
