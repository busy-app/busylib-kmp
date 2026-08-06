## No Anonymous Tuples

Do not use `Pair` or `Triple` for typed/domain data.

Use a small named `data class` instead, with meaningful property names.

Example:

```kotlin
// Bad
List<Pair<String, Double>>

// Good
data class MuTypedTuple(val name: String, val value: Double)
List<MuTypedTuple>
```
