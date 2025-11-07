plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()
}
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.ktx)
        implementation(projects.components.log)
        implementation(projects.components.di)

        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.connectionbuilder.api)
        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.transportconfigbuilder.api)

        implementation(libs.kotlin.coroutines)
    }
}
