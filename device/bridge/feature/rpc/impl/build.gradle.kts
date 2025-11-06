plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.feature.rpc.api)

    implementation(projects.di)
    implementation(projects.ktx)
    implementation(projects.log)

    implementation(projects.device.bridge.feature.common.api)
    implementation(projects.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.kotlin.serialization.json)
}
