plugins {
    id("flipper.multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)

        api(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)

        implementation(libs.ktor.client.core)
    }
}
