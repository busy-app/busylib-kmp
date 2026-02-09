package net.flipper.property

import localProperties
import org.gradle.api.GradleException
import org.gradle.api.Project

class SecretPropertyValue(
    private val project: Project,
    override val key: String
) : PropertyValue {

    /**
     * System.getenv doesn't allow dots
     */
    private val envKey: String = key.replace(".", "_")

    override fun getValue() = runCatching {
        // try to get system ci property
        val systemEnvProperty = System.getenv(envKey)
        if (systemEnvProperty != null) return@runCatching systemEnvProperty
        project.logger.debug("System.enviroment $envKey is missing. Getting it from local.properties")
        // if not ci getting from local.properties
        return@runCatching project.localProperties
            .getProperty(key)
            ?: throw GradleException("Required property $key not defined!")
    }
}
