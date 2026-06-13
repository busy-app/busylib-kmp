---
name: using-metro-di
description: Use when adding or changing dependency-injection wiring in busylib-kmp with Metro (dev.zacsweers.metro) — contributing a binding, binding one class to several interfaces, set/map multibindings, feature-factory or transport map entries, qualifiers, scoped singletons, assisted injection, or the platform dependency graphs.
---

# Using Metro DI in busylib-kmp

## Overview

This project uses **Metro DI** (`dev.zacsweers.metro`), a compiler-plugin DI framework, with a single graph scope: the marker class `net.flipper.busylib.core.di.BusyLibGraph`. There is no other scope. Bindings are **contributed** (Anvil-style) from any module and merged into the four platform graphs in `entrypoint/`.

Three project rules override Metro defaults:
1. **Always pass the bound type explicitly** as `binding<T>()` — even with a single supertype.
2. **Platform `@DependencyGraph` interfaces are `internal`** (SKIE/ObjC reason — see Gotchas).
3. **Metro stays ≤ 1.1.x** while Kotlin is 2.3.21 — see [[metro-version-ceiling]].

The Metro Gradle plugin is applied to every module via the `flipper.metro-multiplatform` convention plugin; you only write Kotlin annotations. No KSP, no manual `@Module`/component classes.

## Quick reference

| Case | Pattern |
|---|---|
| Bind impl → one interface | `@Inject @ContributesBinding(BusyLibGraph::class, binding<Api>()) class Impl : Api` |
| Bind impl → two interfaces | repeat `@ContributesBinding(...)` per interface (same scope) |
| Add to a `Set<T>` multibinding | `@ContributesIntoSet(BusyLibGraph::class, binding<T>())` |
| Add to the feature map `Map<FDeviceFeature, FDeviceFeatureApi.Factory>` | `@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>()) @FDeviceFeatureKey(FDeviceFeature.X)` on the Factory class |
| Add to a `Map<KClass<*>, V>` (transports) | `@Provides @IntoMap @ClassKey(FXConfig::class)` in a `@ContributesTo @BindingContainer object` |
| Provide a value (has a body / 3rd-party type) | `@Provides` fn in a `@ContributesTo @BindingContainer object` |
| Singleton (one instance per graph) | `@SingleIn(BusyLibGraph::class)` on the class or `@Provides` fn |
| Qualifier | `@Qualifier`-meta annotation; **function-site** at provision, **param-site** at injection |
| Assisted injection | `@AssistedInject` class + nested `@AssistedFactory` |
| Runtime-supplied factory of `T` | inject `() -> T` (the project's `Provider<T>` typealias) |
| Platform graph | `internal @DependencyGraph(BusyLibGraph::class) interface` + `@DependencyGraph.Factory` + `createGraphFactory<…>()` |

Import everything from `dev.zacsweers.metro` (one symbol per import, no wildcard): `Inject`, `Provides`, `SingleIn`, `ContributesTo`, `ContributesBinding`, `ContributesIntoSet`, `ContributesIntoMap`, `BindingContainer`, `ClassKey`, `IntoMap`, `MapKey`, `Qualifier`, `Assisted`, `AssistedInject`, `AssistedFactory`, `DependencyGraph`, `binding`, `createGraphFactory`.

## Cases

### 1. Bind an implementation to an interface
```kotlin
@Inject
@ContributesBinding(BusyLibGraph::class, binding<EventBusApi>())
class EventBusApiImpl : EventBusApi
```
Two interfaces → repeat the annotation (same scope each time):
```kotlin
@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<EventBusApi>())
@ContributesBinding(BusyLibGraph::class, binding<BusyLibEventPublisher>())
class EventBusApiImpl : EventBusApi, BusyLibEventPublisher
```

### 2. Set multibinding (e.g. startup listeners)
```kotlin
@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<InternalBUSYLibStartupListener>())
class BUSYLibNameWatcher(/* injected deps */) : InternalBUSYLibStartupListener
```
Consumed as `Set<InternalBUSYLibStartupListener>`.

### 3. Feature-factory map (enum-keyed)
The factory class for a device feature contributes itself into `Map<FDeviceFeature, FDeviceFeatureApi.Factory>` using the custom `@FDeviceFeatureKey` map key (declared in `bridge/feature/common/api`). No companion/`Component` interface.
```kotlin
@Inject
@ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
@FDeviceFeatureKey(FDeviceFeature.ABOUT)
class Factory : FDeviceFeatureApi.Factory { /* override suspend fun invoke(...) */ }
```
To define a new enum-keyed map, add the enum value to `FDeviceFeature` and reuse `@FDeviceFeatureKey`. A *new* key type needs its own `@MapKey annotation class` whose `@Target` includes at least `FUNCTION` (Metro rejects map keys that don't allow FUNCTION).

### 4. KClass-keyed map (transports → `Map<KClass<*>, DeviceConnectionApiHolder>`)
Values are wrapped (not the bound class itself), so use a provider in a binding container, not class-level contribution:
```kotlin
@ContributesTo(BusyLibGraph::class)
@BindingContainer
object MockDeviceConnectionModule {
    @Provides
    @IntoMap
    @ClassKey(FMockDeviceConnectionConfig::class)
    fun getMockDeviceConnection(api: MockDeviceConnectionApi): DeviceConnectionApiHolder =
        DeviceConnectionApiHolder(api)
}
```

### 5. `@Provides` modules (bodies, 3rd-party types, qualifiers)
Convert what would be a kotlin-inject `interface` module into a `@ContributesTo @BindingContainer object`:
```kotlin
@ContributesTo(BusyLibGraph::class)
@BindingContainer
object KtorModule {
    @Provides
    @SingleIn(BusyLibGraph::class)
    @KtorNetworkClientQualifier
    fun provideKtorNetworkHttpClient(): HttpClient = getHttpClient()
}
```

### 6. Qualifiers
Declare the qualifier with Metro's meta-annotation; allow both function and parameter targets:
```kotlin
@Qualifier
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS, AnnotationTarget.TYPE
)
annotation class KtorNetworkClientQualifier
```
- **Provision:** put it on the `@Provides` **function** (`@Provides @KtorNetworkClientQualifier fun …(): HttpClient`), NOT on the return type. Metro reads declaration-site annotations, not type-site.
- **Injection:** put it **on the parameter** (`@KtorNetworkClientQualifier client: HttpClient`). Typealiases are NOT implicit qualifiers in Metro.

### 7. Assisted injection
`@AssistedInject` on the class, `@Assisted` on the runtime-supplied params, a nested `@AssistedFactory`. Metro matches factory method params to constructor `@Assisted` params **by name** (and order). The `@AssistedInject` class must NOT carry `@SingleIn` or any `@Contributes*` (Metro FIR error).

Same-module use → plain factory:
```kotlin
@AssistedInject
class Foo(@Assisted scope: CoroutineScope, private val dep: Bar) {
    @AssistedFactory
    fun interface Factory { operator fun invoke(scope: CoroutineScope): Foo }
}
```
Cross-module (factory interface lives in an `:api` module) → put the `@ContributesBinding` on the nested factory, binding the API-side factory interface:
```kotlin
@AssistedInject
class FBSBDeviceApiImpl(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val connectedDevice: FConnectedDeviceApi,
    private val factories: Map<FDeviceFeature, FDeviceFeatureApi.Factory>,
) : FBSBDeviceApi {
    @AssistedFactory
    @ContributesBinding(BusyLibGraph::class, binding<FBSBDeviceApi.Factory>())
    fun interface Factory : FBSBDeviceApi.Factory {
        override fun invoke(scope: CoroutineScope, connectedDevice: FConnectedDeviceApi): FBSBDeviceApiImpl
    }
}
```
Do NOT use metro-utils `@ContributesAssistedFactory` — it is JVM-only and pinned to an incompatible Metro version.

### 8. Provider / Lazy
The project keeps `typealias Provider<T> = () -> T` (`core/di/ProviderKtx.kt`). Inject `() -> T` (or the `Provider<T>` alias) for a deferred/repeatable factory; Metro supplies it natively. Use `kotlin.Lazy<T>` for once-only lazy. Do NOT switch to `dev.zacsweers.metro.Provider` (it changes the exported Swift type and emits a warning).

### 9. Platform graphs (entrypoint)
One graph per platform in `entrypoint/src/<platform>Main`. **Must be `internal`** (see Gotchas). Root inputs are `@Provides` params on the factory; instantiate via `createGraphFactory`.
```kotlin
@DependencyGraph(BusyLibGraph::class)
internal interface BUSYLibGraphIOS {
    val busyLib: BUSYLibIOS
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides scope: CoroutineScope, /* … */): BUSYLibGraphIOS
    }
}
// in BUSYLibIOS.build():
val graph = createGraphFactory<BUSYLibGraphIOS.Factory>().create(scope, /* … */)
```
No explicit `@SingleIn` is needed on the graph (the scope arg implies it). No expect/actual `create`. Contributions from common code merge automatically per target — there are no workaround modules.

## Gotchas (project-specific, all hard-won)

- **`binding<T>()` is mandatory and positional.** `@ContributesBinding(BusyLibGraph::class, binding<Api>())`. Never the implicit single-supertype form, never the kotlin-inject `Api::class` form.
- **Graphs MUST be `internal`.** A public `@DependencyGraph` exports Metro's merged `bindIntoMap…`/binding methods into the ObjC/Swift header, breaking the XCFramework link (`cannot find protocol declaration`) and tripping SKIE's consistency verifier. `internal` keeps DI plumbing out of the Swift surface; the public `BUSYLib*.build()` is the only entry point.
- **Metro version ceiling = 1.1.x** with Kotlin 2.3.21 — 1.2.0+ native klibs have ABI 2.4.0 and the K/N 2.3.21 compiler rejects them (JVM compiles fine and hides it). See [[metro-version-ceiling]].
- **`@AssistedInject` classes are never scoped or contributed.** Move any binding to the nested `@AssistedFactory`.
- **One top-level class per file** (detekt). A new `@MapKey` annotation, qualifier, or binding container each gets its own file.
- `:api` modules still must not depend on `:rpc:api`, return `CResult<T>`, and expose `WrappedFlow`/`WrappedStateFlow` (AGENTS.md) — Metro doesn't change those rules.

## Common mistakes

| Mistake | Fix |
|---|---|
| `@ContributesBinding(BusyLibGraph::class, Api::class)` (kotlin-inject style) | `binding<Api>()` |
| `@ContributesBinding(BusyLibGraph::class, binding = binding<Api>())` | drop the `binding =` label — positional `binding<Api>()` |
| `@Provides @IntoMap fun(): Pair<K, V>` | Metro has no Pair-keyed maps; use `@ClassKey`/custom `@MapKey` + return `V` |
| Returning the bound type qualified: `fun x(): @Qual HttpClient` | qualifier on the **function**: `@Provides @Qual fun x(): HttpClient` |
| Public platform graph | make it `internal` |
| `@Contributes*`/`@SingleIn` on an `@AssistedInject` class | move it to the nested `@AssistedFactory` |
| New `@MapKey` without a `FUNCTION` target | include `AnnotationTarget.FUNCTION` (Metro requires it) |

## Verify

```bash
./gradlew :entrypoint:compileKotlinJvm        # merges & validates the whole graph (fast)
./gradlew detektFormat                        # one-class-per-file, formatting
./gradlew allTests                            # all platforms
./gradlew :entrypoint:assembleBusyLibKMPDebugXCFramework   # SKIE/Swift surface (Mac)
```
A missing/duplicate binding or a leaked graph symbol surfaces at `:entrypoint` compile or the XCFramework link, not in the contributing module.
