package net.flipper.busylib.core.wrapper

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