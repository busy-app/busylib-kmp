package net.flipper.property

import org.gradle.api.Project

/**
 * Load property from any source (secret or gradle property)
 * @see GradlePropertyValue
 * @see SecretPropertyValue
 */
class AnyPropertyValue(
    private val project: Project,
    override val key: String,
) : PropertyValue {
    override fun getValue(): Result<String> {
        val secretProperty = SecretPropertyValue(project, key).getValue()
        if (secretProperty.isSuccess) return secretProperty
        return GradlePropertyValue(project, key).getValue()
    }
}
