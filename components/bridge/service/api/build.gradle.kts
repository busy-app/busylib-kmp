plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
