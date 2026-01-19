plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)

        api(projects.components.bridge.feature.common.api)

        implementation(libs.kotlin.coroutines)
    }
}
