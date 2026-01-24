plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlinx-serialization")
}
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.config.api)

        implementation(projects.components.core.log)
        implementation(projects.components.core.di)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.klibs.kstorage)
        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
    }
}
