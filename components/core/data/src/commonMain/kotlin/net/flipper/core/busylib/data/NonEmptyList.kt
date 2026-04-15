package net.flipper.core.busylib.data

data class NonEmptyList<T> internal constructor(
    val head: T,
    internal val tail: List<T>
) : List<T> by listOf(head) + tail

fun <T> nonEmptyListOf(head: T, vararg tail: T): NonEmptyList<T> {
    return NonEmptyList(head, tail.toList())
}

fun <T> nonEmptyListOf(head: T, tail: List<T>): NonEmptyList<T> {
    return NonEmptyList(head, tail)
}
