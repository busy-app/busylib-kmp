package net.flipper.property

import org.gradle.api.Project

class GradlePropertyValue(
    private val project: Project,
    override val key: String,
) : PropertyValue {
    override fun getValue(): Result<String> {
        return runCatching {
            project.providers.gradleProperty(key).get()
        }
    }
}
