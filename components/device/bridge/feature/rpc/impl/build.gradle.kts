plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.device.bridge.feature.rpc.api)

    implementation(projects.components.di)
    implementation(projects.components.ktx)
    implementation(projects.components.log)

    implementation(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.kotlin.serialization.json)
}
