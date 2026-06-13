package net.flipper.bridge.connection.feature.common.api

import dev.zacsweers.metro.MapKey

/**
 * Map key for contributing [FDeviceFeatureApi.Factory] implementations into the
 * `Map<FDeviceFeature, FDeviceFeatureApi.Factory>` multibinding.
 */
@MapKey
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE
)
annotation class FDeviceFeatureKey(val value: FDeviceFeature)
