plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.components.device.bridge.feature.link.api)

    implementation(projects.components.principal.api)

    implementation(projects.components.di)
    implementation(projects.components.ktx)
    implementation(projects.components.log)

    implementation(projects.components.device.bridge.feature.common.api)
    implementation(projects.components.device.bridge.transport.common.api)

    implementation(projects.components.device.bridge.feature.rpc.api)

    implementation(libs.kotlin.coroutines)
}
