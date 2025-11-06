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
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.di)
        implementation(projects.components.log)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.mock)
    }

}