# BUSY Lib description for agents

You are a highly skilled expert in Android and Kotlin coroutines with decades of experience. You have identified countless bugs and know exactly where errors typically occur.

## What this library is

**BUSY Lib KMP** is a Kotlin Multiplatform SDK for controlling [BUSY Bar](https://busy.bar) devices. It is published as Maven artifacts (group `net.flipper.busylib.kmp`) and as an `XCFramework` named `BusyLibKMP` for Apple platforms. It abstracts multi-transport device communication (BLE / LAN TCP / Cloud WebSocket) behind a single feature-based API.

**Consumers:**
- iOS app: `/Users/lionzxy/flipper/iOS` (Swift/Xcode, consumes the `BusyLibKMP` XCFramework via SPM binary target in `Bridge/Package.swift`).
- Android/Desktop app: `/Users/lionzxy/flipper/BSBAndroid` (multi-module Gradle, consumes `net.flipper.busylib.kmp:entrypoint` + `core-network` from the Flipper Reposilite Maven).

Changes to the public API of this library affect both consumers. When changing exported types, consider SKIE/Swift interop first — see the Swift interop section below.

## Targets & build

- **KMP targets**: `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`, `macosX64`, `jvm` (desktop). macOS targets are opt-in via `flipper.macOSEnabled` in `local.properties`.
- **Kotlin**: 2.3.10 (pinned to SKIE 0.10.11 compatibility — see https://skie.touchlab.co/intro#compatibility-with-kotlin before bumping).
- **Coroutines**: 1.10.2. **AGP**: 9.1.0. **compileSdk/targetSdk**: 36, **minSdk**: 26. **Java target**: 17.
- **Version catalog**: `gradle/libs.versions.toml`.
- **Custom Gradle convention plugins** live in `build-logic/plugins/convention/`:
  - `flipper.multiplatform` — base KMP configuration
  - `flipper.multiplatform-compose` — Compose Multiplatform
  - `flipper.anvil-multiplatform` — kotlin-inject + Anvil DI
  - `flipper.publish` — Maven publishing
  - Detekt integration (`net.flipper.busylib.detekt`)

## Running Gradle

Common tasks:
- `./gradlew allTests` — run all tests (common + platform).
- `./gradlew detekt` — lint / custom rules.
- `./gradlew :entrypoint:assembleBusyLibKMPDebugXCFramework` — build iOS XCFramework.
- `./gradlew :entrypoint:copyXCFrameworkDebug` — copy XCFramework into the iOS Xcode project (requires `flipper.iosProjectBridgeAbsolutePath` + `flipper.iosProjectAbsolutePath` in `local.properties`).

## Module layout (orient yourself here)

Top-level entry point: `entrypoint/` — exports the `BUSYLib` interface and the per-platform builders (`BUSYLibAndroid.build(...)`, `BUSYLibIOS.Companion.build(...)`, `BUSYLibMacOS.build(...)`, `BUSYLibDesktop.build(...)`).

`components/` is split by concern:

- **`components/core/`** — infrastructure with no domain knowledge:
  - `core:wrapper` — Swift-interop types: `CResult<T>` (sealed Success/Failure), `WrappedFlow<T>`, `WrappedStateFlow<T>`, `WrappedSharedFlow<T>`, `.wrap()` extensions.
  - `core:di` — kotlin-inject + Anvil graph root (`BusyLibGraph`).
  - `core:log` — `TaggedLogger` + `LogTagProvider` (Timber on Android, `AppleLogger` on Apple).
  - `core:ktx` — `exponentialRetry`, coroutine helpers.
  - `core:ktor` — HTTP engine (Darwin / OkHttp / CIO by platform).
  - `core:network` — `BUSYLibNetworkStateApi` (host app provides implementation).
  - `core:timezone`, `core:data` (`NonEmptyList<T>`), `core:buildkonfig`.

- **`components/bridge/`** — device protocol:
  - `bridge:config:{api,impl,internal}` — `FDevicePersistedStorage`, `BUSYBar` model with `ConnectionWay` sealed children (`BLE`, `Cloud`, `Lan`, `Mock`). Transaction-based persistence.
  - `bridge:device:{common,bsb,firmware-update,firstpair/connection}` — device model + BSB protocol + firmware update + initial pairing (Android-only).
  - `bridge:transport/` — one package per transport, each with `api`/`impl`:
    - `ble` — Nordic Semiconductor library on Android, CoreBluetooth on iOS.
    - `tcp:lan` — local TCP (JVM + macOS; default host 10.0.4.20).
    - `tcp:cloud` — Ktor WebSocket relay via cloud.busy.app.
    - `combined` — priority-based fallback (LAN → Cloud → BLE).
    - `mock` — for testing / dev.
    - `common` — `FConnectedDeviceApi`, connection status listeners.
  - `bridge:feature:{info,wifi,ble,settings,timezone,link,battery,screen-streaming,firmware-update,events,oncall,smarthome,finish-setup,about,provider,rpc,common}` — each has `api` + `impl`. Features extend `FDeviceFeatureApi` and are discovered via `FFeatureProvider.get<T>()` / `.getSync<T>()`.
  - `bridge:orchestrator:{api,impl,internal}` — `FDeviceOrchestrator` state machine: `Disconnected` → `Connecting` → `Connected` → `Offline`.
  - `bridge:service:{api,impl}` — `FConnectionService` (`forgetDevice`, `forceRefreshConnection`).
  - `bridge:bsbprotobuf` — Wire-generated protobuf definitions.

- **`components/cloud/`** — `cloud:rest` (firmware directory, auth tokens) and `cloud:barsws` (WebSocket cloud relay protocol).

- **`components/principal/api`** — `BUSYLibPrincipalApi` (host app provides user/auth state).

- **`components/tools/`** — `oncall`, `multistream`.

- **`components/watchers/`** — background observers (`changename`, `provisioning`, `desktop`).

- **`detekt-rules/`** — custom rules enforced in CI:
  - `SerialNameNotProvidedRule` — every `@Serializable` field needs `@SerialName`.
  - `ForbiddenApiModuleDependencyRule` — `:api` modules must not depend on `:rpc:api`.
  - `RunCatchingInSuspendRule` — flags `runCatching` inside `suspend` functions.
  - `FilterIsInstanceWithGenericsRule` — unsafe generic filter detection.
  - `ApiWrappedTypeRule` — API modules must use `CResult` / `WrappedFlow` wrappers.

- **`sample/android`**, **`sample/cmp`**, **`appleApp/`** — sample apps.

## Public API shape

`net.flipper.busylib.BUSYLib`:
```kotlin
interface BUSYLib {
    val connectionService: FConnectionService
    val orchestrator: FDeviceOrchestrator
    val featureProvider: FFeatureProvider
    val firmwareUpdaterApi: FirmwareUpdaterApi
    val persistedStorage: FDevicePersistedStorage
    val multiStreamApi: MultiStreamApi
    fun launch()
}
```

Every caller supplies its own `CoroutineScope` (usually `SupervisorJob`-backed). Lifecycle is scope-based — cancel the scope to tear everything down.

Features are discovered dynamically:
```kotlin
val wifi: FWiFiFeatureApi? = busyLib.featureProvider.getSync<FWiFiFeatureApi>()
val flow: WrappedFlow<FFeatureStatus<FWiFiFeatureApi>> = busyLib.featureProvider.get<FWiFiFeatureApi>()
```

## Hard rules for API modules

- Return `CResult<T>` from `suspend` functions, never Kotlin's inline `Result<T>` (it does not cross the Swift boundary).
- Expose `WrappedFlow<T>` / `WrappedStateFlow<T>` / `WrappedSharedFlow<T>`, never bare `Flow` / `StateFlow` (SKIE `FlowInterop` is intentionally disabled).
- `:api` modules must not depend on `:rpc:api` — protobuf/RPC is an implementation detail (enforced by `ForbiddenApiModuleDependencyRule`).
- `@Serializable` data classes must annotate every field with `@SerialName`, including fields that "look obvious" (enforced by `SerialNameNotProvidedRule`).
- No `runCatching` inside `suspend` functions — it swallows `CancellationException` (enforced by `RunCatchingInSuspendRule`). Use explicit try/catch that rethrows `CancellationException`.
- Export new `:api` modules from `entrypoint/build.gradle.kts` inside the `XCFramework("BusyLibKMP")` block, otherwise Swift cannot see them.

## DI (kotlin-inject + Anvil)

- Single graph root: `BusyLibGraph`.
- Contribute with `@ContributesTo(BusyLibGraph::class)` + `@Provides`.
- Register features via an `@IntoMap` of `FDeviceFeature -> FDeviceFeatureApi.Factory`.
- Constructor-inject with `@Inject`. No service locator, no manual singletons.

## Swift interop (SKIE)

Configured in `entrypoint/build.gradle.kts`:
- `SuspendInterop.Enabled(true)` — `suspend fun` → Swift `async throws`.
- `SealedInterop.Enabled(true)` — sealed classes → Swift enums (iOS uses `onEnum(of:)` to pattern-match).
- `EnumInterop.Enabled(true)`.
- `FlowInterop.Enabled(false)` — **intentional**; use the `Wrapped*Flow` types. iOS bridges them to `AsyncStream` via a `.watch(onEach, onComplete, onError) -> Closeable` method (see `/Users/lionzxy/flipper/iOS/Bridge/Sources/Bridge/Wrapper/Flow+AsyncStream.swift`).

iOS consumer shape (for reference when designing APIs):
- The Swift Bridge module at `/Users/lionzxy/flipper/iOS/Bridge/Sources/Bridge/` re-wraps KMP sealed classes as Swift enums for `Sendable` + `Equatable` ergonomics (e.g. `ApiFBleStatus` → `PeripheralBleStatus`).
- It implements `BUSYLibPrincipalApi`, `BUSYLibNetworkStateApi`, and `AppleLogger` from the Swift side — these are the injection points.
- Initialization: `BUSYLibIOS.Companion().build(scope:principalApi:observableSettings:manager:hostApi:networkStateApi:)` in `PeripheralContainer.swift`.
- XCFramework is distributed as a GitHub-released binary zip, pinned by version + SHA256 in `iOS/Bridge/Package.swift`. iOS does not invoke Gradle.

Android consumer shape:
- Depends on Maven coordinates `net.flipper.busylib.kmp:entrypoint` and `:core-network` from `https://reposilite.flipp.dev/releases` (+ `-debug` variants from `/snapshots`).
- Variant selection lives in `build-logic/plugins/convention/.../versionCatalog.kt`: `flipper.use_local_busylib=true` → `busylibLocal`, else debug/release per flavor.
- Metro (Anvil-style) DI provides a single `BUSYLib` singleton; wrappers in `components/bsb/device/bridge/` expose `connectionService`, `orchestrator`, `featureProvider`, `firmwareUpdaterApi` as `@Provides`.
- Android-side implementations of `BUSYLibPrincipalApi`, `BUSYLibAndroidNetworkStateApi`, `BUSYLibHostApi` live in `components/bsb/{auth/principal/impl, device/bridge/src/androidMain}`.

## Dev guidelines

- **One class per file.** No multiple top-level classes in a single Kotlin file.
- Package naming mirrors module path: `net.flipper.bridge.connection.feature.<name>.{api,impl}`.
- Prefer `kotlinx.collections.immutable` for public collections.
- Logging: implement `LogTagProvider` + use `TaggedLogger(TAG)`, not `println` / `Log.d`.
- Serialization: `kotlinx.serialization` with JSON (Ktor negotiation); protobuf via Wire 6.1.0 for device protocol.
- Retries: use `exponentialRetry { ... }` from `core:ktx` rather than hand-rolling loops.
- Thread-safety in transport/HTTP code: recent fixes added `RWMutex` for concurrent engine access — preserve this pattern.

## Testing

When writing a test, make sure that:
- You are testing the expected business logic, not the actual one.
- Test coverage for new tests MUST always be 100%.
- The aim of the tests is to find a bug, not to reproduce the current behavior.
- If you're not sure what the business logic of the class is, ask the user.
- Feel free to modify base classes to improve their testability, but ask the user first about all changes in base class.

Focus on multithreading and race conditions corner cases.

**Test layout**: `src/commonTest/`, `src/androidHostTest/` (Robolectric), `src/iosTest/`, `src/macosTest/`, `src/jvmTest/`. Frameworks: `kotlin.test`, JUnit Jupiter 6.x, MockK 1.14.x, Robolectric 4.16.x.

To run tests: `./gradlew allTests`

## CI

GitHub Actions in `.github/workflows/`:
- `pr.yml` runs Gradle-wrapper validation → detekt → `allTests` → Compose sample build → publishes PR artifacts to Reposilite as version `pr-{NUMBER}` → posts a comment with download links and XCFramework hash.
- `release.yml` + `call-release-*.yml` handle Maven Central, internal Reposilite, and XCFramework release publishing.

When a PR CI run fails: check detekt output (custom rules listed above are the most frequent cause), then `allTests`, then sample validation (API breakage usually surfaces here).

**Before every `git commit` or `git push`**, invoke the `preparing-for-pr-ci-checks` skill (at `.claude/skills/preparing-for-pr-ci-checks/SKILL.md`) — it encodes the exact detekt→tests→publish loop that mirrors `pr.yml` and must be followed in order.
