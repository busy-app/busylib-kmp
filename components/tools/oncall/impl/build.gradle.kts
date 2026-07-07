plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.tools.oncall.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.ktor)
        implementation(projects.components.core.log)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.lanmonitor.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.transport.tcp.cloud.impl)
        implementation(projects.components.bridge.transport.tcp.lan.impl)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.oncall.api)
        implementation(projects.components.bridge.feature.oncall.impl)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.rpc.impl)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.negotiation)
        implementation(libs.ktor.serialization)
    }
}
