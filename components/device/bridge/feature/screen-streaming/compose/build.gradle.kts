plugins {
    id("flipper.multiplatform-compose")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.components.device.bridge.feature.screenStreaming.api)

    implementation(projects.components.ktx)
    implementation(projects.components.log)

    implementation(libs.kotlin.coroutines)
}
