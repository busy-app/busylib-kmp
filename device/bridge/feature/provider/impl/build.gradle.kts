plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.feature.provider.api)

    implementation(projects.di)
    implementation(projects.ktx)

    implementation(projects.device.bridge.config.api)
    implementation(projects.device.bridge.device.common.api)
    implementation(projects.device.bridge.device.bsb.api)
    implementation(projects.device.bridge.feature.common.api)
    implementation(projects.device.bridge.orchestrator.api)
    implementation(projects.device.bridge.transport.common.api)

    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.coroutines)
}
