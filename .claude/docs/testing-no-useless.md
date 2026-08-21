# Testing Guidelines

Do not write tests that only verify language/framework behavior or trivial data storage.

Avoid tests like:

```kotlin
data class MyClass(val value: Int)

val clazz = MyClass(10)
assertEquals(10, clazz.value)
```

This only tests that Kotlin `data class` properties work.

Write tests only when they verify meaningful project behavior, such as:

* business logic
* validation rules
* edge cases
* error handling
* integration between components
* regressions for known bugs

Before adding a test, ask: **Would this test fail if our project logic were broken?**

If the answer is no, do not write the test.
