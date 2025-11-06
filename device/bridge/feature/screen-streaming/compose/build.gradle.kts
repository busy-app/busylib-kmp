plugins {
    id("flipper.multiplatform-compose")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.device.bridge.feature.screenStreaming.api)

    implementation(projects.ktx)
    implementation(projects.log)

    implementation(libs.kotlin.coroutines)
}
