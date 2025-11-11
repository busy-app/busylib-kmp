package net.flipper

import net.flipper.property.GradlePropertyValue
import net.flipper.property.asCached
import net.flipper.property.exception.PropertyValueNotPresentException
import org.gradle.api.Project

object Config {
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
                return FlavorType.DEBUG
            }
            return flavor
        }
}
