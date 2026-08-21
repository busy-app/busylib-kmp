## Kotlin Timestamp and Duration Rule

When writing Kotlin code, do not represent dates, timestamps, or durations as raw primitive or text types.

Avoid using:

```kotlin
Long
Int
String
```

for datetime-related values.

### Bad

```kotlin
val createdAt: Long
val expiresAt: String
val timeoutSeconds: Int
```

### Good

```kotlin
import kotlin.time.Duration
import kotlin.time.Instant

val createdAt: Instant
val expiresAt: Instant
val timeout: Duration
```

Use `kotlin.time.Instant` for exact moments in time.

Use `kotlin.time.Duration` for time intervals, delays, cooldowns, timeouts, and expiration lengths.

Only convert timestamps or durations to `Long`, `Int`, or `String` at external boundaries, such as database storage, JSON serialization, logging, or API compatibility layers.
