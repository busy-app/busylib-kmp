import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
kotlin {
    sourceSets.all {
        optIns.onEach(languageSettings::optIn)
    }
    compilerOptions {
        optIn.addAll(optIns)

        // This is required due to our modules hierarchy
        // Without this, modules named impl/api will have
        // same name kotlin_module
        freeCompilerArgs.addAll(
            "-module-name",
            project
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
// This is required due to our modules hierarchy
// Without this, modules named impl/api will have
// same name artifactId
afterEvaluate {
    if (plugins.hasPlugin("com.vanniktech.maven.publish")) {
        extensions.configure<MavenPublishBaseExtension> {
            val artifactId = project.path
                .replace(":components:", "")
                .replace(":", "-")
                .replace(".", "-")
            coordinates(null, artifactId, null)
        }
    }
}
