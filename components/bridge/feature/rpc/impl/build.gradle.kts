plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.rpc.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        implementation(projects.components.core.buildkonfig)
        implementation(projects.components.core.ktor)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.client.websockets)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.serialization)
        implementation(libs.kotlin.serialization.json)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
        implementation(libs.ktor.client.mock)
    }
}
