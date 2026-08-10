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
    settings = SharedPreferencesSettings(
        context.getSharedPreferences("settings", MODE_PRIVATE)
    ),
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

## Developing

### How to clone

- For clone: `git clone --recurse-submodules git@github.com:busy-app/busylib-kmp.git`
- For update submodules: `git submodule update --init --recursive`

### Creating local build

Create a `local.properties` in the repository root and execute gradle commands. The file is
git-ignored, so it is the place for per-developer overrides.

| Property                               | Default | Example                          | Effect                                                                                                                    |
|----------------------------------------|---------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `flipper.appleEnabled`                 | `true`  | `flipper.appleEnabled=false`     | Registers the iOS and macOS targets. Set to `false` on non-Apple machines.                                                |
| `flipper.macOSEnabled`                 | `true`  | `flipper.macOSEnabled=false`     | Registers the `macosArm64` / `macosX64` targets only, leaving iOS alone.                                                  |
| `flipper.signPublications`             | `true`  | `flipper.signPublications=false` | Signs Maven publications. Must be `false` to run `./gradlew publishToMavenLocal` without signing keys.                    |
| `flipper.iosProjectBridgeAbsolutePath` | —       | `/Users/me/git/iOS/Bridge`       | Where `./gradlew :entrypoint:copyXCFrameworkDebug` copies the XCFramework.                                                |
| `flipper.iosProjectAbsolutePath`       | —       | `/Users/me/git/iOS/`             | Xcode project root used by the same task. Both paths must be set for it to run.                                           |
| `current_flavor_type`                  | `DEBUG` | `current_flavor_type=DEVELOP`    | BuildKonfig flavor: `DEBUG`, `DEVELOP` or `PROD`. `PROD` drops verbose/sensitive logging, mocks and dev firmware channel. |

`flipper.appleEnabled`, `flipper.macOSEnabled` and `current_flavor_type` are also accepted as Gradle
properties (`gradle.properties` or `-P`). `flipper.signPublications` and the two path properties are
read only from `local.properties` or from the environment with dots replaced by underscores
(`flipper_signPublications`), because they are CI secrets.

Every `klibs.*` key from `gradle.properties` — project coordinates, version, Android SDK and Java
levels — can be overridden the same way; `local.properties` wins over `gradle.properties`.

For xcode don't forget:

- Resolve dependencies: `xcodebuild -resolvePackageDependencies`
- Clean your xcode build before launch