plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        api(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.serialization.json)
    }
}
