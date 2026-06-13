plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.mock)
    }
}
