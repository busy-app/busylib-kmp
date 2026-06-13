@file:Suppress("Filename")

import net.flipper.property.AnyPropertyValue
import net.flipper.property.SecretPropertyValue
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

// Workaround for https://github.com/gradle/gradle/issues/15383
val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

val Project.appleEnabled: Boolean
    get() = AnyPropertyValue(this, "flipper.appleEnabled")
        .getValue()
        .getOrNull()
        ?.toBoolean()
        ?: true

val Project.macOSEnabled: Boolean
    get() = AnyPropertyValue(this, "flipper.macOSEnabled")
        .getValue()
        .getOrNull()
        ?.toBoolean()
        ?: true

val Project.signPublications: Boolean
    get() = SecretPropertyValue(this, "flipper.signPublications")
        .getValue()
        .getOrNull()
        ?.toBoolean()
        ?: true
