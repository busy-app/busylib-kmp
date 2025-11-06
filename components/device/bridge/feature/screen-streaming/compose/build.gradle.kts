plugins {
    id("flipper.multiplatform-compose")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.bsb.device.bridge.feature.screenStreaming.api)

    implementation(projects.components.core.ktx)
    implementation(projects.components.core.log)

    implementation(libs.kotlin.coroutines)
}
