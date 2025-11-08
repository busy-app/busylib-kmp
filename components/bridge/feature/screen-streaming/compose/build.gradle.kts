plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.kotlin.multiplatform.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    androidLibrary {}

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
