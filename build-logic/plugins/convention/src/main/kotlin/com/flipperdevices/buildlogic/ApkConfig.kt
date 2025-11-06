package com.flipperdevices.buildlogic

import com.flipperdevices.buildlogic.model.FlavorType
import com.flipperdevices.buildlogic.property.GradlePropertyValue
import com.flipperdevices.buildlogic.property.exception.PropertyValueNotPresentException
import com.flipperdevices.buildlogic.property.SecretPropertyValue
import com.flipperdevices.buildlogic.property.asCached
import org.gradle.api.Project

object ApkConfig {
    const val APPLICATION_ID = "com.flipperdevices.busybar"

    const val MIN_SDK_VERSION = 26

    const val TARGET_SDK_ANDROID_VERSION = 36
    const val TARGET_SDK_WEAROS_VERSION = 35
    const val COMPILE_SDK_VERSION = 36
    const val ROBOELECTRIC_SDK_VERSION = 34


    val Project.VERSION_CODE
        get() = GradlePropertyValue(rootProject, "version_code")
            .asCached(rootProject.extensions)
            .getValue()
            .getOrNull()
            ?.toIntOrNull()
            ?: 1

    val Project.VERSION_NAME
        get() = GradlePropertyValue(rootProject, "flipper.major_version")
            .asCached(rootProject.extensions)
            .getValue()
            .getOrThrow()
            .plus(".")
            .plus(VERSION_CODE)

    val Project.DISABLE_NATIVE: Boolean
        get() = SecretPropertyValue(rootProject, "flipper.disable_native")
            .asCached(rootProject.extensions)
            .getValue()
            .getOrNull()
            ?.toBooleanStrictOrNull()
            ?: true

    val Project.CURRENT_FLAVOR_TYPE: FlavorType
        get() {
            val property = GradlePropertyValue(rootProject, "current_flavor_type")
                .asCached(rootProject.extensions)
            val propertyValue = property
                .getValue()
                .onFailure { exception ->
                    if (exception !is PropertyValueNotPresentException) {
                        logger.error("Property ${property.key} was not found, writing default")
                    }
                }
            val flavor = propertyValue
                .map { value -> FlavorType.entries.find { it.name.equals(value, true) } }
                .getOrNull()
            if (flavor == null) {
                logger.error("Not found ${propertyValue.getOrNull()} in flavors")
                if (propertyValue.getOrNull() != null) {
                    error("Not allowed to use wrong flavor type name!")
                }
                return FlavorType.DEV
            }
            return flavor
        }
}
