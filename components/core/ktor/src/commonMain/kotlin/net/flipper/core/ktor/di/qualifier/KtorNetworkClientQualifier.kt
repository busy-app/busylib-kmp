package net.flipper.core.ktor.di.qualifier

import dev.zacsweers.metro.Qualifier

@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE
)
annotation class KtorNetworkClientQualifier
