plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
}

commonDependencies {
    implementation(projects.device.bridge.feature.link.api)

    implementation(projects.principal.api)

    implementation(projects.di)
    implementation(projects.ktx)
    implementation(projects.log)

    implementation(projects.device.bridge.feature.common.api)
    implementation(projects.device.bridge.transport.common.api)

    implementation(projects.device.bridge.feature.rpc.api)

    implementation(libs.kotlin.coroutines)
}
