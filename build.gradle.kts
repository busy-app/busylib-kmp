import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.kotlin.dsl.findByType
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

    alias(libs.plugins.klibs.gradle.detekt) apply false
    alias(libs.plugins.klibs.gradle.detekt.compose) apply false
    alias(libs.plugins.klibs.gradle.dokka.root) apply false
    alias(libs.plugins.klibs.gradle.dokka.module) apply false
    alias(libs.plugins.klibs.gradle.java.core) apply false
    alias(libs.plugins.klibs.gradle.publication) apply false
    alias(libs.plugins.klibs.gradle.rootinfo) apply false
    alias(libs.plugins.klibs.gradle.android.core) apply false
    alias(libs.plugins.klibs.gradle.android.compose) apply false
    alias(libs.plugins.klibs.gradle.android.apk.sign) apply false
    alias(libs.plugins.klibs.gradle.android.apk.name) apply false
    alias(libs.plugins.klibs.gradle.android.namespace) apply false
}

apply(plugin = "ru.astrainteractive.gradleplugin.detekt")

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
                    freeCompilerArgs.addAll(
                        "-module-name",
                        this@afterEvaluate
                            .path
                            .replace(":components:", "")
                            .replace(":entrypoint", "entrypoint")
                            .replace(":", "_")
                            .replace("-", "_")
                            .replace(".", "_")
                    )
                    optIn.addAll(optIns)
                }
            }
    }
}

subprojects.forEach { subProject ->
    val artifactId = subProject.path
        .replace(":components:", "")
        .replace(":entrypoint", "entrypoint")
        .replace(":", "-")
        .replace(".", "-")
    val isEntryPoint = subProject.name == "entrypoint"
    if (!isEntryPoint && !subProject.path.startsWith(":components")) return@forEach
    subProject.apply(plugin = "ru.astrainteractive.gradleplugin.publication")
    subProject
        .extensions
        .configure<MavenPublishBaseExtension> {
            coordinates(null, artifactId, null)
        }
}
