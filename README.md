# BUSY Lib for Kotlin

![](docs/platforms.svg)

This is a Kotlin Multiplatform library for working with [busy.bar](https://busy.bar)

> ⚠️ Work in progress, deep alpha. API subject to change in the future.

## How to use

1. Provide platform-specific dependencies for BUSY Lib initialization
```kotlin
val busyLib = BUSYLibAndroid.build(
    scope = CoroutineScope(SupervisorJob()),
    principalApi = UserPrincipalApiNoop(),
    bsbBarsApi = BSBBarsApiNoop(),
    persistedStorage = persistedStorage,
    context = this
)
```
2. Init connection service
```kotlin
busyLib.connectionService.onApplicationInit()
```
3. Use BUSY Lib:
```kotlin
val deviceInfo = busyLib
    .featureProvider
    .getSync<FDeviceInfoFeatureApi>()
    .getDeviceInfo()
```