plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.feature.provider.api)

    implementation(projects.components.core.di)
    implementation(projects.components.core.ktx)

    implementation(projects.components.bsb.device.bridge.config.api)
    implementation(projects.components.bsb.device.bridge.device.common.api)
    implementation(projects.components.bsb.device.bridge.device.bsb.api)
    implementation(projects.components.bsb.device.bridge.feature.common.api)
    implementation(projects.components.bsb.device.bridge.orchestrator.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.coroutines)
}
