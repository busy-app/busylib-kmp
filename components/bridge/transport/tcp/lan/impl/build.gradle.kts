plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.components.bridge.transport.common.api)
            implementation(projects.components.bridge.transport.common.impl)
            implementation(projects.components.bridge.transport.tcp.common)
            implementation(projects.components.bridge.transport.tcp.lan.api)
            implementation(projects.components.core.di)
            implementation(projects.components.core.log)

            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)
        }
    }
}
