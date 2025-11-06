plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.feature.info.api)

    implementation(projects.components.core.di)
    implementation(projects.components.core.ktx)
    implementation(projects.components.core.log)

    implementation(projects.components.bsb.device.bridge.feature.common.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(projects.components.bsb.device.bridge.feature.rpc.api)

    implementation(libs.kotlin.coroutines)
}
