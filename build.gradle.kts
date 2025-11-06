import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.metro) apply false

    // klibs - core
    alias(libs.plugins.klibs.gradle.detekt) apply false
    alias(libs.plugins.klibs.gradle.detekt.compose) apply false
    alias(libs.plugins.klibs.gradle.dokka.root) apply false
    alias(libs.plugins.klibs.gradle.dokka.module) apply false
    alias(libs.plugins.klibs.gradle.java.core) apply false
    alias(libs.plugins.klibs.gradle.publication) apply false
    alias(libs.plugins.klibs.gradle.rootinfo) apply false
    // klibs - android
    alias(libs.plugins.klibs.gradle.android.core) apply false
    alias(libs.plugins.klibs.gradle.android.compose) apply false
    alias(libs.plugins.klibs.gradle.android.apk.sign) apply false
    alias(libs.plugins.klibs.gradle.android.apk.name) apply false
    alias(libs.plugins.klibs.gradle.android.namespace) apply false
}

apply(plugin = "ru.astrainteractive.gradleplugin.root.info")
apply(plugin = "ru.astrainteractive.gradleplugin.detekt")

subprojects.forEach { subProject ->
    subProject.apply(plugin = "ru.astrainteractive.gradleplugin.publication")
    subProject.plugins.withId("org.jetbrains.kotlin.jvm") {
        subProject.apply(plugin = "ru.astrainteractive.gradleplugin.java.core")
    }
    subProject.plugins.withId("com.android.library") {
        subProject.apply(plugin = "ru.astrainteractive.gradleplugin.android.core")
        subProject.apply(plugin = "ru.astrainteractive.gradleplugin.android.namespace")
    }
}
val optIns = listOf(
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

subprojects.forEach { subProject ->
    subProject.afterEvaluate {
        extensions
            .findByType<KotlinMultiplatformExtension>()
            ?.sourceSets
            ?.all {
                optIns.onEach(languageSettings::optIn)
            }
        tasks
            .withType<KotlinCompile>()
            .configureEach {
                compilerOptions {
                    optIn.addAll(optIns)
                }
            }
    }
}
