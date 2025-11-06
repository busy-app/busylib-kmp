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
        implementation(projects.components.bridge.feature.rpc.api)

        implementation(projects.components.di)
        implementation(projects.components.ktx)
        implementation(projects.components.log)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.serialization)
        implementation(libs.kotlin.serialization.json)
    }
}
