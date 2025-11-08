plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.kotlin.multiplatform.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidLibrary {}
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

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
