package net.flipper.busylib.core.wrapper

/**
 * Custom wrapper over operation result (success/failure).
 *
 * Created specifically for Swift interoperability in Kotlin Multiplatform projects.
 *
 * Standard Kotlin [Result] is not exported to Swift/Objective-C because:
 * - [Result] is an inline value class, which cannot be represented in Objective-C runtime
 * - Swift cannot see or use Kotlin [Result] type in function signatures
 * - This makes it impossible to return [Result] from Kotlin code consumed by Swift
 *
 * [CResult] solves this by being a regular sealed class that:
 * - Is properly exported to Swift as a native Swift enum with associated values
 * - Can be used as return type in functions called from Swift
 * - Can be stored in class properties visible to Swift
 * - Provides idiomatic Swift API through [Success] and [Failure] cases
 * - Can be converted to Kotlin [Result] when needed via [toKotlinResult]
 */
sealed class CResult<out T> {
    data class Success<T>(val value: T) : CResult<T>()
    data class Failure(val error: Throwable) : CResult<Nothing>()

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Failure -> error
    }

    inline fun onFailure(action: (Throwable) -> Unit): CResult<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun onSuccess(action: (T) -> Unit): CResult<T> {
        if (this is Success) action(value)
        return this
    }

    companion object {
        fun <T> success(value: T): CResult<T> = Success(value)
        fun <T> failure(error: Throwable): CResult<T> = Failure(error)
    }

    fun toKotlinResult(): Result<T> = when (this) {
        is Success -> Result.success(value)
        is Failure -> Result.failure(error)
    }
}
