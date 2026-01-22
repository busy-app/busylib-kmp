package net.flipper.property

import gradleProperties
import org.gradle.api.GradleException
import org.gradle.api.Project

class GradlePropertyValue(
    private val project: Project,
    override val key: String,
) : PropertyValue {
    private fun getFromProvidersOrNull(): String? {
        return project.providers
            .gradleProperty(key)
            .orNull
    }

    private fun getFromGradlePropertiesOrNull(): String? {
        return project.gradleProperties.getProperty(key)
    }

    override fun getValue(): Result<String> {
        return runCatching {
            getFromProvidersOrNull()
                ?: getFromGradlePropertiesOrNull()
                ?: throw GradleException("Required property $key not defined!")
        }
    }
}
