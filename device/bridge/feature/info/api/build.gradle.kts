plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}
commonDependencies {
    implementation(projects.ktx)

    api(projects.device.bridge.feature.common.api)

    implementation(libs.kotlin.coroutines)
}
