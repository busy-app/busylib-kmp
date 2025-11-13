plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.ble.common)
        implementation(projects.components.bridge.transport.common.impl)

        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.cio)
    }
}
