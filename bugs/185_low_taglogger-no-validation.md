# `TaggedLogger(TAG)` accepts any string as tag without validation; Android tag length and char restrictions are not enforced

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/log/src/commonMain/kotlin/net/flipper/core/busylib/log/TaggedLogger.kt`
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/log/src/androidMain/kotlin/net/flipper/core/busylib/log/LogPlatform.android.kt`

## Summary
`class TaggedLogger(override val TAG: String) : LogTagProvider` accepts any string. On Android, `Timber.tag(tag)` ultimately delegates to `Log.println` whose tag length is limited to 23 characters on API < 26 (`Log.isLoggable` enforces this with `IllegalArgumentException`). Some code paths in this codebase (visible in tests like `TaggedLogger("TransformWhileSubscribedSharedFlow")` — 33 chars) exceed that limit on older Android versions.

```kotlin
class TaggedLogger(override val TAG: String) : LogTagProvider
```

No validation, no truncation. Timber 5.x normally truncates to 23 chars on legacy Android, so the *crash* may be avoided, but log lines for `TransformWhileSubscribedSharedFlow` will appear under the truncated tag `TransformWhileSubscribed` — collisions with other long tags become invisible.

## Root Cause
No defensive truncation, no documentation of the constraint.

## Impact
- On API < 26 Android, long tags collide after truncation (e.g. `TransformWhileSubscribedSharedFlow` and a hypothetical `TransformWhileSubscribedStateFlow` both truncate to the same 23-char prefix).
- Engineers debugging on old Android devices see unexpected log groupings.

## Suggested Fix
Validate / truncate at construction time:
```kotlin
class TaggedLogger(rawTag: String) : LogTagProvider {
    override val TAG: String = rawTag.take(MAX_ANDROID_TAG_LENGTH)
    companion object { private const val MAX_ANDROID_TAG_LENGTH = 23 }
}
```
Or at least add a debug assertion:
```kotlin
init { check(rawTag.length <= 23) { "TAG too long for legacy Android: $rawTag" } }
```
