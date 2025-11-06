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
    sourceSets.androidMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.ble.api)

        implementation(projects.components.log)
        implementation(projects.components.di)
        implementation(projects.components.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ble.client)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.cio)

        implementation(libs.fastutil)
    }
}
