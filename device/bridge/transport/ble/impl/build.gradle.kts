plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

androidDependencies {
    implementation(projects.device.bridge.transport.common.api)
    implementation(projects.device.bridge.transport.common.impl)
    implementation(projects.device.bridge.transport.ble.api)

    implementation(projects.log)
    implementation(projects.di)
    implementation(projects.ktx)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.ble.client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(libs.fastutil)
}
