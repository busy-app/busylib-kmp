plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.transport.common.api)
    implementation(projects.device.bridge.transport.common.impl)
    implementation(projects.device.bridge.transport.mock.api)
    implementation(projects.di)
    implementation(projects.log)

    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.mock)
}
