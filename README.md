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

2. Init BUSY Lib background services

```kotlin
busyLib.launch()
```

3. Use BUSY Lib:

```kotlin
val deviceInfo = busyLib
    .featureProvider
    .getSync<FDeviceInfoFeatureApi>()
    .getDeviceInfo()
```

## Creating local build

Create `local.properties` and execute gradle commands

```properties
# Disable macOS/apple if needed
flipper.macOSEnabled=false
flipper.appleEnabled=false
# Disable signing for publications
# ./gradlew publishToMavenLocal
flipper.signPublications=false
# If want to use xcFramework, add destination, where it should be copied
# Then run ./gradlew :entrypoint:copyXCFrameworkDebug
flipper.iosProjectBridgeAbsolutePath=/Users/makeevrserg/Desktop/git/iOS/Bridge
# If want more verbose logging, add this
current_flavor_type=DEVELOP
```

For xcode don't forget:

- Resolve dependencies: `xcodebuild -resolvePackageDependencies`
- Clean your xcode build before launch