plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.ble.common)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }
}
