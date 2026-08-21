## Constructors Dependency Injection Rule

Never instantiate concrete dependencies inside service/class constructors, constructor default parameters, fields, or methods.

Dependencies must be created only at the composition root / DI module / application wiring layer, then passed down from parent to child.

Bad:

```kotlin
class LivePredictionService(
    private val repo: MarketDataRepository,
    private val pipeline: FeatureLabelPipeline = FeatureLabelPipeline(),
    private val model: DirectionModel = TemperatureCalibratedDirectionModel(),
    private val clock: Clock = Clock.System
)
```

Good:

```kotlin
class LivePredictionService(
    private val repo: MarketDataRepository,
    private val pipeline: FeatureLabelPipeline,
    private val model: DirectionModel,
    private val clock: Clock
)
```

When adding a new dependency, update the root wiring instead of adding `= SomeImplementation()` in the class.
