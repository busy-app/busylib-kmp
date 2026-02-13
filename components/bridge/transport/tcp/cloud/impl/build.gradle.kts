plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
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
            implementation(projects.components.core.ktx)
            implementation(projects.components.core.wrapper)

            implementation(projects.components.cloud.api)
            implementation(projects.components.cloud.barsws.api)
            implementation(projects.components.principal.api)

            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlin.serialization.json)
        }
    }
}
