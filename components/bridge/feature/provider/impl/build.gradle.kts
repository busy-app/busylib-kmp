plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.provider.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.wrapper)
        implementation(projects.components.core.ktx)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.device.common.api)
        implementation(projects.components.bridge.device.bsb.api)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.coroutines)
    }
}
