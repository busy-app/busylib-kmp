plugins {
    id("flipper.multiplatform")
    id("kotlinx-serialization")
    id("flipper.metro-multiplatform")
}
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
        implementation(libs.settings.test)
    }
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.config.internal)

        implementation(projects.components.core.log)
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.klibs.kstorage)
        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
    }
}
