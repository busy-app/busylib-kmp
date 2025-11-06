package com.flipperdevices.core.ktx.common

/**
 * Sometimes we need to show shimmers for resources, which is still loading
 */
sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val throwable: Throwable? = null) : Resource<Nothing>()
}

fun <T> Result<T>.asResource(): Resource<T> = fold(
    onSuccess = { Resource.Success(it) },
    onFailure = { Resource.Error(it) }
)

val <T> Resource<T>.dataOrNull: T?
    get() = (this as? Resource.Success<T>)?.data
