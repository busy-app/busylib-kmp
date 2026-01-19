plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
