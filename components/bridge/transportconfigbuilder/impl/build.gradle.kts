plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transportconfigbuilder.api)

        implementation(projects.components.di)

        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.immutable)
    }
}
