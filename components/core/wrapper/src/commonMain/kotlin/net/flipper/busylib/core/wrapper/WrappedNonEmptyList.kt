package net.flipper.busylib.core.wrapper

import net.flipper.core.busylib.data.NonEmptyList

data class WrappedNonEmptyList<T>(
    val origin: NonEmptyList<T>
) {
    fun getHead(): T = origin.head
    fun getList(): List<T> = origin

    override fun toString(): String {
        return origin.toString()
    }
}

fun <T> NonEmptyList<T>.wrap() = WrappedNonEmptyList(this)
