plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

androidDependencies {
    implementation(projects.components.device.bridge.transport.common.api)
    implementation(projects.components.device.bridge.transport.common.impl)
    implementation(projects.components.device.bridge.transport.ble.api)

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
