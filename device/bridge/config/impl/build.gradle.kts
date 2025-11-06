plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
    id("kotlinx-serialization")
}
commonDependencies {
    implementation(projects.device.bridge.config.api)

    implementation(projects.log)
    implementation(projects.di)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.klibs.kstorage)
    implementation(libs.settings)
    implementation(libs.settings.observable)
    implementation(libs.settings.coroutines)
}
