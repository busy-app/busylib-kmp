@file:Suppress("Filename")

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

// Workaround for https://github.com/gradle/gradle/issues/15383
val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

fun Project.includeCommonKspConfigurationTo(
    vararg toConfigurations: String,
) {
    pluginManager.withPlugin("com.google.devtools.ksp") {
        val commonKsp = configurations.create("commonKsp")
        toConfigurations.forEach { configurationName ->
            configurations.getByName(configurationName).extendsFrom(commonKsp)
        }
    }
}
