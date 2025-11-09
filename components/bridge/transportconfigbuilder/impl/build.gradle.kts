plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
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
