## No Nested Classes Rule

Do not declare classes inside other classes.

All domain models, DTOs, services, helpers, factories, and implementations must be top-level declarations.

Bad — nested non-sealed class:

```kotlin
// PredictionModel.kt
class PredictionModel {
    data class Config(
        val threshold: Double
    )
}
```

Good — separate top-level classes in separate files:

```kotlin
// PredictionModelConfig.kt
data class PredictionModelConfig(
    val threshold: Double
)
```

```kotlin
// PredictionModel.kt
class PredictionModel
```

```kotlin
// PredictionModelTrainer.kt
class PredictionModelTrainer
```

Kotlin `inner class` is strictly forbidden.

Bad:

```kotlin
// PredictionModel.kt
class PredictionModel {
    inner class Trainer
}
```

Allowed exception: sealed hierarchies may be nested when the parent is a `sealed interface` or `sealed class`.

Allowed — one file:

```kotlin
// PredictionResult.kt
sealed interface PredictionResult {
    data class Success(val value: Double) : PredictionResult
    data class Failed(val reason: String) : PredictionResult
}
```

Allowed — one file:

```kotlin
// PredictionResult.kt
sealed interface PredictionResult

data class SuccessPredictionResult(
    val value: Double
) : PredictionResult

data class FailedPredictionResult(
    val reason: String
) : PredictionResult
```

Do not use nested classes for grouping. Only sealed hierarchy variants may be nested or kept together in one file.
