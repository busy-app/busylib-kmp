plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.device.bridge.feature.provider.api)

    implementation(projects.components.di)
    implementation(projects.components.ktx)

    implementation(projects.components.device.bridge.config.api)
    implementation(projects.components.device.bridge.device.common.api)
    implementation(projects.components.device.bridge.device.bsb.api)
    implementation(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.orchestrator.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.coroutines)
}
