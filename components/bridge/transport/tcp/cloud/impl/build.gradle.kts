plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.components.bridge.transport.common.api)
            implementation(projects.components.bridge.transport.common.impl)
            implementation(projects.components.bridge.transport.tcp.cloud.api)
            implementation(projects.components.core.ktor)
            implementation(projects.components.core.di)
            implementation(projects.components.core.log)

            implementation(projects.components.cloud.barsws.api)

            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)
        }
    }
}
