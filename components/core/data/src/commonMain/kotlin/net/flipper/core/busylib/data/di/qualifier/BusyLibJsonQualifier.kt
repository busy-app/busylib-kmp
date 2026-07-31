package net.flipper.core.busylib.data.di.qualifier

import dev.zacsweers.metro.Qualifier

/**
 * The shared lenient [kotlinx.serialization.json.Json] of the library:
 * forward-compatible parsing (unknown keys ignored) and full encoding
 * (defaults written). Use it instead of creating ad-hoc Json instances.
 */
@Qualifier
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE
)
annotation class BusyLibJsonQualifier
