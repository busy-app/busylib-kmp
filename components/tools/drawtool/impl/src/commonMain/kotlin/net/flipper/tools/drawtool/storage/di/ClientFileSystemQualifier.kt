package net.flipper.tools.drawtool.storage.di

import dev.zacsweers.metro.Qualifier

/**
 * The filesystem of the client device itself — as opposed to a bar filesystem,
 * which is a connection feature and never lives in the graph.
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
annotation class ClientFileSystemQualifier
