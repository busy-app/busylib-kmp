# BUSY Lib description for agents

You are a highly skilled expert in Android and Kotlin coroutines with decades of experience. You have identified countless bugs and know exactly where errors typically occur.

## What this library is

**BUSY Lib KMP** is a Kotlin Multiplatform SDK for controlling [BUSY Bar](https://busy.bar) devices. It is published as Maven artifacts (group `net.flipper.busylib.kmp`) and as an `XCFramework` named `BusyLibKMP` for Apple platforms. It abstracts multi-transport device communication (BLE / LAN TCP / Cloud WebSocket) behind a single feature-based API.

Changes to the public API of this library affect both consumers. When changing exported types, consider SKIE/Swift interop first

## Targets & build

- **KMP targets**: `android`, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `macosArm64`, `macosX64`, `jvm` (desktop). macOS targets are opt-in via `flipper.macOSEnabled` in `local.properties`.
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

## Module layout

- `entrypoint/` — public `BUSYLib` + per-platform builders: `BUSYLibAndroid`, `BUSYLibIOS`, `BUSYLibMacOS`, `BUSYLibDesktop`.
- `components/core/` — infra: `wrapper` (`CResult`, `Wrapped*Flow`, `.wrap()`), `di` (kotlin-inject/Anvil root `BusyLibGraph`), `log` (`TaggedLogger`), `ktx` (`exponentialRetry`), `ktor` (HTTP), `network` (`BUSYLibNetworkStateApi`), `timezone`, `data` (`NonEmptyList`), `buildkonfig`.
- `components/bridge/` — device protocol:
  - `config` — `FDevicePersistedStorage`, `BUSYBar` + `ConnectionWay.{BLE,Cloud,Lan,Mock}`.
  - `device` — `common`, `bsb`, `firmware-update`, `firstpair/connection` (Android-only).
  - `transport/{ble,tcp:lan,tcp:cloud,combined,mock,common}` — `combined` does LAN → Cloud → BLE fallback; `tcp:lan` default host `10.0.4.20`.
  - `feature/{info,wifi,ble,settings,timezone,link,battery,screen-streaming,firmware-update,events,oncall,smarthome,finish-setup,about,provider,rpc,common}` — each split `api`/`impl`; resolved via `FFeatureProvider.get<T>()` / `.getSync<T>()`.
  - `orchestrator` — `FDeviceOrchestrator` state machine (`Disconnected`/`Connecting`/`Connected`/`Offline`).
  - `service` — `FConnectionService` (`forgetDevice`, `forceRefreshConnection`).
  - `bsbprotobuf` — Wire-generated protobuf.
- `components/cloud/` — `rest` (firmware dir, auth) + `barsws` (cloud WebSocket relay).
- `components/principal/api` — `BUSYLibPrincipalApi` (host app supplies user/auth state).
- `components/tools/` — `oncall`, `multistream`. `components/watchers/` — `changename`, `provisioning`, `desktop`.
- `detekt-rules/` — `SerialNameNotProvidedRule`, `ForbiddenApiModuleDependencyRule`, `RunCatchingInSuspendRule`, `FilterIsInstanceWithGenericsRule`, `ApiWrappedTypeRule` (see "Hard rules" below).
- `sample/android`, `sample/cmp`, `appleApp/` — samples.

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

## Dev guidelines

- **One class per file.** No multiple top-level classes in a single Kotlin file.
- Package naming mirrors module path: `net.flipper.bridge.connection.feature.<name>.{api,impl}`.
- Prefer `kotlinx.collections.immutable` for public collections.
- Logging: implement `LogTagProvider` + use `TaggedLogger(TAG)`, not `println` / `Log.d`.
- Serialization: `kotlinx.serialization` with JSON (Ktor negotiation); protobuf via Wire 6.1.0 for device protocol.
- Retries: use `exponentialRetry { ... }` from `core:ktx` rather than hand-rolling loops.

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
