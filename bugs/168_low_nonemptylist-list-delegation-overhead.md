# `NonEmptyList` delegates to `listOf(head) + tail`, allocating a fresh list on every method call via `: List<T> by …`

## Type
infrastructure

**Severity:** low

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/data/src/commonMain/kotlin/net/flipper/core/busylib/data/NonEmptyList.kt` lines 3–10

## Summary
```kotlin
data class NonEmptyList<T> internal constructor(
    val head: T,
    internal val tail: List<T>
) : List<T> by listOf(head) + tail {
    override fun toString(): String = toList().toString()
}
```

The `by` delegate target is `listOf(head) + tail`. Kotlin re-evaluates the delegate target on every constructor call, so each `NonEmptyList` instance materialises a fresh `List<T>` *once* at construction (good — `by` captures the expression's result). However, **`override fun toString()` calls `toList()`**, which is the `List<T>.toList()` extension. On a `List<T>`-by-delegate, `toList()` allocates a *new* `ArrayList` copy of every call.

That extension also makes equality / hashCode delegate to the inner list (because `data class` would generate them, but `List<T> by …` overrides — actually `data class` generates equals/hashCode based on declared properties only: `head` and `tail`). So `==` checks `head == head && tail == tail`, not the delegate list.

Subtle issue: `data class` and interface delegation interact. Two `NonEmptyList` with the same `head` and `tail` are `equals` (good), but their hash codes are computed off `head` and `tail`, not off the delegate. **A `List<T>`-typed reference comparing two `NonEmptyList`s via `Objects.equals` may use the delegate's `equals`, which calls `AbstractList.equals(other)` and walks both lists — also correct.

The actual bug: `toString()` calls `toList()` and then `.toString()` on the resulting copy. This is O(N) allocation for every diagnostic print. For long lists embedded in log statements (frequently used in BLE feature code), this is wasteful.

A more direct (and equivalent) implementation would be:
```kotlin
override fun toString(): String = (listOf(head) + tail).toString()
```
Or store the materialised list once and reuse it.

## Root Cause
`toString()` triggers `toList()` (defensive copy) before `toString()`. Unnecessary allocation.

## Impact
- Excess GC pressure for `NonEmptyList<X>.toString()` in tight log loops.
- No correctness issue.

## Suggested Fix
Either:
1. `override fun toString(): String = "[$head${tail.joinToString(prefix=", ", separator=", ") { it.toString() }}]"` (custom formatting, no allocation).
2. Store the materialised list and reuse:
   ```kotlin
   private val materialised: List<T> = buildList(tail.size + 1) { add(head); addAll(tail) }
   override fun toString(): String = materialised.toString()
   ```
