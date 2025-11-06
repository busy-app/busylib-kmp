plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.screenStreaming.api)

        implementation(projects.components.ktx)
        implementation(projects.components.log)

        implementation(libs.kotlin.coroutines)

        implementation(kotlin.compose.runtime)
        implementation(kotlin.compose.ui)
    }
}
