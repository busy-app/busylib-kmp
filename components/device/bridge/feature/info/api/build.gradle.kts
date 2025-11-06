plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}
commonDependencies {
    implementation(projects.components.core.ktx)

    api(projects.components.bsb.device.bridge.feature.common.api)

    implementation(libs.kotlin.coroutines)
}
