plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.connectionbuilder.api)

        implementation(projects.components.di)

        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.mock.api)

        implementation(libs.kotlin.coroutines)
    }
}
