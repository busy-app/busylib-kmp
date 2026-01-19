plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    // id("ru.astrainteractive.gradleplugin.android.namespace") // Temporarily disabled for AGP 9.0.0 compatibility
    // id("ru.astrainteractive.gradleplugin.android.core") // Temporarily disabled for AGP 9.0.0 compatibility
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transportconfigbuilder.api)

        implementation(projects.components.core.di)

        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.tcp.cloud.api)
        implementation(projects.components.bridge.transport.tcp.lan.api)
        implementation(projects.components.bridge.config.api)

        implementation(libs.kotlin.immutable)
    }
}
