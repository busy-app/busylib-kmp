## Data Classes Default Values Rule

Do not add default values to constructor parameters of data classes.

Bad:

```kotlin
data class PredictionInput(
    val symbol: String = "",
    val window: Int = 100,
    val features: List<Double> = emptyList()
)
```

Good:

```kotlin
data class PredictionInput(
    val symbol: String,
    val window: Int,
    val features: List<Double>
)
```

Allowed exception: data classes marked with `@Serializable`, where defaults are required for serialization/backward compatibility.

```kotlin
@Serializable
data class PredictionInputDto(
    val symbol: String = "",
    val window: Int = 100,
    val features: List<Double> = emptyList()
)
```

If a data class is not serialized, do not use default constructor values. Pass values explicitly from the caller/root wiring.
