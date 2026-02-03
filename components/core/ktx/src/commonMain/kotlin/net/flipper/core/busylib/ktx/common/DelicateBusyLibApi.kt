package net.flipper.core.busylib.ktx.common

@MustBeDocumented
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate API and its use requires care." +
        " Make sure you fully read and understand documentation of the declaration that is marked as a delicate API."
)
@Retention(value = AnnotationRetention.BINARY)
annotation class DelicateBusyLibApi
