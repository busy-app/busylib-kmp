plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("kotlinx-serialization")
}
commonDependencies {
    implementation(projects.components.core.ktx)
    implementation(projects.components.core.data)

    api(projects.components.bsb.device.bridge.feature.common.api)
    implementation(projects.components.bsb.device.bridge.transport.common.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.ktor.client.core)
}
