package com.flipperdevices.buildlogic.plugin.mppdependencies

import com.flipperdevices.buildlogic.ApkConfig.DISABLE_NATIVE
import com.flipperdevices.buildlogic.util.ProjectExt.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class FlipperMultiplatformDependenciesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.kotlin.sourceSets.forEach { sourceSet ->
            val noMainSourceSetName = sourceSet.name.removeSuffix(MAIN_SOURCE_SET_POSTFIX)
            sourceSet.dependencies {
                target.extensions.create(
                    "${noMainSourceSetName}Dependencies",
                    FlipperMultiplatformDependenciesScope::class,
                    this
                )
            }
        }
        if (target.DISABLE_NATIVE) {
            listOf(
                "iosArm64",
                "ios",
                "iosSimulatorArm64",
                "iosX64"
            ).forEach { noMainSourceSetName ->
                target.extensions.create(
                    "${noMainSourceSetName}Dependencies",
                    FakeFlipperMultiplatformDependenciesScope::class,
                )
            }
        }
    }

    companion object {
        /**
         * The postfix of default source set naming aka commonMain, androidMain etc
         */
        private const val MAIN_SOURCE_SET_POSTFIX = "Main"
    }
}
