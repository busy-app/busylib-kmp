## Kotlin Lambda Naming Rule

When writing Kotlin code, never use the implicit lambda parameter `it`.

Always give lambda parameters explicit, meaningful names.

### Bad

```kotlin
getRequest().let {
    println("$it")
}
```

### Good

```kotlin
getRequest().let { request ->
    println("$request")
}
```

Use names that describe the value.
