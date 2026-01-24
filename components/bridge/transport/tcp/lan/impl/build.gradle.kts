plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
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
