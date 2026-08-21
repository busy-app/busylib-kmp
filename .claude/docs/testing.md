## Testing

When writing a test, make sure that:

* Test domain logic without frameworks.
* Test application use cases with fake ports.
* Test adapters and infrastructure with integration tests where appropriate.
* Avoid mocking value objects and simple data classes.
* Prefer fakes over mocks for repositories and gateways when behavior matters.
* Tests should verify expected behavior, not implementation details.
* Do not write tests that simply mirror the current code structure.
* Test names must use `snake_case`.
* Prefer `GIVEN_WHEN_THEN` structure in test names.
* You are testing the expected business logic, not the actual one.
* Prefer splitting code into testable functions/chunks if that doesn't affect the public API.
* Test coverage for new tests MUST always be 100%.
* The aim of the tests is to validate the intended behavior/contract and reveal bugs, not to lock in incidental current behavior or implementation details.
* If you're not sure what the business logic of the class is, ask the user.
* Feel free to modify base classes to improve their testability, but ask the user first about all changes in base class.
* Tests should cover as many scenarios and edge cases as possible without duplicating existing tests.
* Focus on multithreading and race conditions corner cases.
* For tests involving coroutines/reactivity, `cashapp/turbine` can be used, if `kotlinx.coroutines.test` is not enough. Otherwise tests can be flaky.

Example:

```kotlin
@Test
fun GIVEN_empty_cart_WHEN_order_is_created_THEN_returns_cart_is_empty() {
    val useCase = CreateOrderUseCase(
        orderRepository = FakeOrderRepository(),
        paymentGateway = FakePaymentGateway()
    )

    val result = useCase.execute(
        CreateOrderCommand(
            customerId = CustomerId("customer-1"),
            items = emptyList()
        )
    )

    assertEquals(CreateOrderResult.CartIsEmpty, result)
}
```

Good test focus:

```kotlin
@Test
fun GIVEN_existing_active_customer_WHEN_order_is_created_THEN_order_is_saved()
```

Bad test focus:

```kotlin
@Test
fun GIVEN_create_order_WHEN_execute_called_THEN_repository_save_called_once()
```

The second test verifies implementation detail. The first verifies the expected behavior of the use case.

Example fake:

```kotlin
class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<UserId, User>()

    override suspend fun findById(id: UserId): User? {
        return users[id]
    }

    override suspend fun save(user: User): SaveUserResult {
        users[user.id] = user
        return SaveUserResult.Success
    }
}
```

**Test layout**: `src/test/kotlin`

**Frameworks**: `kotlin.test`, `kotlinx.coroutines.test`

**To run tests on kotlin-jvm:** `./gradlew test`
**To run tests on kotlin-multiplatform:** `./gradlew allTests`