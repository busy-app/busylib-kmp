## Kotlin: Error Handling — `Result`

Never use exceptions for normal control flow in your own application logic. Functions that can fail should return Kotlin’s standard `Result<T>` instead of throwing.

Kotlin’s `Result` is not `Result<T, E>`. It has only one type parameter:

```kotlin
Result<T>
```

Failures are represented internally as `Throwable`, not as a typed error channel. Therefore, domain errors should be modeled as specific exception classes, preferably sealed when they belong to the same domain.

```kotlin
sealed class OrderError(message: String) : Exception(message)

class OrderParseError(
    message: String
) : OrderError(message)

class OrderValidationError(
    message: String
) : OrderError(message)
```

Create successful and failed results with the existing Kotlin API:

```kotlin
fun parseOrder(raw: String): Result<Order> {
    return if (raw.isBlank()) {
        Result.failure(OrderParseError("Order payload is blank"))
    } else {
        Result.success(Order(/* ... */))
    }
}
```

Use `runCatching` only at boundaries where code may throw:

```kotlin
fun loadOrder(id: OrderId): Result<Order> =
    runCatching {
        thirdPartyClient.fetchOrder(id.value).toDomain()
    }.recoverCatching { error ->
        throw OrderLoadError("Could not load order ${id.value}", error)
    }
```

`runCatching` catches thrown exceptions and converts them into `Result.failure`.

Consume `Result` with `fold`, `onSuccess`, `onFailure`, or explicit checks.

Use `fold` when you need to produce one final value:

```kotlin
val response: HttpResponse =
    result.fold(
        onSuccess = { order ->
            HttpResponse.ok(order)
        },
        onFailure = { error ->
            HttpResponse.badRequest(error.message ?: "Invalid order")
        }
    )
```

Use `onSuccess` and `onFailure` for side effects such as logging:

```kotlin
result
    .onSuccess { order ->
        logger.info("Loaded order ${order.id}")
    }
    .onFailure { error ->
        logger.warn("Could not load order", error)
    }
```

Use `map` to transform a success value.

```kotlin
val total: Result<Money> =
    parseOrder(raw)
        .map { order -> order.total }
```

Use `mapCatching` only when the transform itself may throw and you want thrown exceptions to become `Result.failure`.

```kotlin
val total: Result<Money> =
    parseOrder(raw)
        .mapCatching { order -> calculateTotal(order) }
```

To chain fallible operations, use `mapCatching` when the next operation throws:

```kotlin
val result: Result<Money> =
    parseOrder(raw)
        .mapCatching { order ->
            validateOrder(order)
            order.total
        }
```

Where `validateOrder` throws a domain-specific exception on failure:

```kotlin
fun validateOrder(order: Order) {
    if (order.items.isEmpty()) {
        throw OrderValidationError("Order must contain at least one item")
    }
}
```

Alternatively, if both functions already return `Result`, combine them explicitly with `fold`:

```kotlin
val result: Result<Money> =
    parseOrder(raw).fold(
        onSuccess = { order ->
            validateOrder(order).map { validOrder ->
                validOrder.total
            }
        },
        onFailure = { error ->
            Result.failure(error)
        }
    )
```

Where `validateOrder` returns `Result<Order>`:

```kotlin
fun validateOrder(order: Order): Result<Order> {
    return if (order.items.isEmpty()) {
        Result.failure(OrderValidationError("Order must contain at least one item"))
    } else {
        Result.success(order)
    }
}
```

Use `recover` or `recoverCatching` to convert a failure into a success fallback.

```kotlin
val total: Result<Money> =
    parseOrder(raw)
        .map { order -> order.total }
        .recover { error ->
            Money.zero()
        }
```

Use `recoverCatching` when recovery itself may throw.

```kotlin
val order: Result<Order> =
    loadOrder(id)
        .recoverCatching { error ->
            loadOrderFromBackup(id).getOrThrow()
        }
```

Avoid `getOrThrow` in normal control flow. It rethrows the failure and defeats the purpose of returning `Result`.

Prefer:

```kotlin
result.fold(
    onSuccess = { value ->
        // handle success
    },
    onFailure = { error ->
        // handle failure
    }
)
```

Avoid:

```kotlin
val value = result.getOrThrow()
```

Use `getOrThrow` only at a hard boundary where throwing is intentional, such as tests, startup validation, or integration with an API that requires exceptions.

Convert third-party exceptions at the boundary. Inner layers should not depend on raw library exceptions.

```kotlin
class OrderLoadError(
    message: String,
    cause: Throwable
) : Exception(message, cause)

fun loadOrder(id: OrderId): Result<Order> =
    runCatching {
        thirdPartyClient.fetchOrder(id.value)
    }.mapCatching { dto ->
        dto.toDomain()
    }.recoverCatching { error ->
        throw OrderLoadError("Could not load order ${id.value}", error)
    }
```

Rules:

* Prefer:
    * `Result<T>` for simple operations where failure does not need rich domain meaning.
    * Nullable types only when absence is the only meaningful failure.
    * A per-use-case `sealed interface` result when different outcomes must be handled explicitly.
* Use `Result.success(value)` and `Result.failure(error)`.
* Use `runCatching` to convert thrown exceptions into `Result`.
* Use `map` for pure success-value transformations.
* Use `mapCatching` when the transformation may throw.
* Use `fold` to handle both success and failure explicitly.
* Use `onSuccess` and `onFailure` for side effects.
* Use `recover` and `recoverCatching` for fallback behavior.
* Do not silently swallow failures.
* Do not use `getOrThrow` to avoid handling errors.
* Make expected failure paths explicit, observable, and testable.
* Do not throw exceptions for expected business or validation failures.
* Exceptions are acceptable for unexpected, unrecoverable, or programmer errors.
* Avoid leaking infrastructure errors into domain or application layers.
* Log failures at the boundary or call site where enough context exists.
* Do not log and rethrow repeatedly.
