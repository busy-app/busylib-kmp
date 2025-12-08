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
    }
    targets.configureEach {
        compilations.configureEach {
            // This is required due to our modules hierarchy
            // Without this, modules named impl/api will have
            // same name kotlin_module
            val moduleName = project
                .path
                .replace(":components:", "")
                .replace(":entrypoint", "entrypoint")
                .replace(":", "_")
                .replace("-", "_")
                .replace(".", "_")
                .trim('_', '-', '.', ' ')

            val fullModuleName = if (compilationName == "main") {
                moduleName
            } else {
                "${moduleName}_${compilationName}"
            }

            compilerOptions.configure {
                freeCompilerArgs.addAll(
                    "-module-name",
                    fullModuleName
                )
            }
        }
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
                .trim('_', '-', '.', ' ')
            coordinates(null, artifactId, null)
        }
    }
}
