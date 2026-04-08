plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.tools.multistream.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)

        implementation(projects.components.cloud.barsws.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.events.api)
        implementation(projects.components.bridge.feature.events.impl)
        implementation(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.screenStreaming.impl)

        implementation(libs.kotlin.coroutines)
    }
}
