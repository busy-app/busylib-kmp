@file:Suppress("Filename")

import com.android.build.gradle.BaseExtension
import com.flipperdevices.buildlogic.ApkConfig
import com.flipperdevices.buildlogic.ApkConfig.VERSION_CODE
import com.flipperdevices.buildlogic.ApkConfig.VERSION_NAME
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun BaseExtension.commonAndroid(target: Project) {
    configureDefaultConfig(target)
    configureBuildFeatures()
}

@Suppress("UnstableApiUsage")
private fun BaseExtension.configureDefaultConfig(project: Project) {
    compileSdkVersion(ApkConfig.COMPILE_SDK_VERSION)
    defaultConfig {
        minSdk = ApkConfig.MIN_SDK_VERSION
        versionCode = project.VERSION_CODE
        versionName = project.VERSION_NAME

        consumerProguardFiles(
            "consumer-rules.pro"
        )

        packagingOptions {
            resources.excludes += "META-INF/LICENSE-LGPL-2.1.txt"
            resources.excludes += "META-INF/LICENSE-LGPL-3.txt"
            resources.excludes += "META-INF/LICENSE-W3C-TEST"
            resources.excludes += "META-INF/DEPENDENCIES"
            resources.excludes += "*.proto"
        }

        testOptions {
            unitTests {
                isIncludeAndroidResources = true
            }
        }
    }
}

@Suppress("ForbiddenComment")
private fun BaseExtension.configureBuildFeatures() {
    //  BuildConfig is java source code. Java and Kotlin at one time affect build speed.
    buildFeatures.buildConfig = false
    buildFeatures.resValues = false
    buildFeatures.shaders = false
}

@Suppress("MaxLineLength")
fun Project.suppressOptIn() {
    extensions
        .findByType<KotlinMultiplatformExtension>()
        ?.sourceSets
        ?.all {
            listOf(
                "kotlin.time.ExperimentalTime",
                "kotlin.uuid.ExperimentalUuidApi"
            )
                .onEach(languageSettings::optIn)

        }

    tasks.withType<KotlinCompile>()
        .configureEach {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")

                optIn.addAll(
                    "com.google.accompanist.pager.ExperimentalPagerApi",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "kotlinx.serialization.ExperimentalSerializationApi",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "com.squareup.anvil.annotations.ExperimentalAnvilApi",
                    "kotlin.time.ExperimentalTime",
                    "kotlin.RequiresOptIn",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "com.google.android.horologist.annotations.ExperimentalHorologistApi",
                    "kotlin.uuid.ExperimentalUuidApi",
                    "androidx.media3.common.util.UnstableApi",
                )
            }
        }
}

fun Project.ignoreNoDiscoveredTests() {
    tasks.withType<AbstractTestTask>().configureEach {
        failOnNoDiscoveredTests = false
    }
}

fun <BuildTypeT : Any> NamedDomainObjectContainer<BuildTypeT>.release(
    action: BuildTypeT.() -> Unit
) {
    maybeCreate("release").action()
}
