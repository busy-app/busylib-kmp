plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.coroutines)
    }
}
