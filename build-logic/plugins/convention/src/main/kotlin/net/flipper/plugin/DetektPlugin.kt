package net.flipper.plugin

import dev.detekt.gradle.Detekt
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireJinfo

class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.plugins) {
            apply("dev.detekt")
        }
        target.tasks.register<Detekt>("detektFormat") {
            autoCorrect.set(true)
        }
        val detektYmlFileName = "detekt.yml"

        val detektFile =
            target.rootProject.layout.buildDirectory.file(detektYmlFileName).get().asFile
        if (!detektFile.exists()) {
            detektFile.parentFile.mkdirs()
            val detektFileResource = Thread.currentThread()
                .getContextClassLoader()
                .getResource(detektYmlFileName)
            val bytes = detektFileResource
                ?.openConnection()
                ?.getInputStream()
                ?.use { it.readBytes() }
                ?: byteArrayOf()
            detektFile.writeBytes(bytes)
        }

        target.tasks.withType<Detekt> {
            // Disable caching
            outputs.upToDateWhen { false }

            reports {
                html.required.set(true)
                checkstyle.required.set(false)
            }
            config.setFrom(detektFile)
            setSource(target.files(target.projectDir))

            include("**/*.kt", "**/*.kts")
            exclude(
                "**/resources/**",
                "**/build/**",
            )
            // Exclude generated sources by absolute path so it works
            // for compilation-based tasks (e.g. detektMainJvm) where
            // detekt v2 sets source roots inside build/generated/
            exclude { it.file.absolutePath.contains("/build/generated/") }

            parallel.set(true)

            buildUponDefaultConfig.set(true)

            allRules.set(true)

            // Target version of the generated JVM bytecode. It is used for type resolution.
            jvmTarget.set(target.requireJinfo.jtarget.majorVersion)
        }

        with(target) {
            dependencies {
                "detektPlugins"(libs.detekt.ruleset.ktlint)
                "detektPlugins"(libs.detekt.ruleset.compose)
                "detektPlugins"(libs.detekt.ruleset.decompose)
            }
        }
    }
}
