plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.transport.common.api)
    implementation(projects.components.bsb.device.bridge.transport.common.impl)
    implementation(projects.components.bsb.device.bridge.transport.mock.api)
    implementation(projects.components.core.di)
    implementation(projects.components.core.log)

    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.mock)
}
