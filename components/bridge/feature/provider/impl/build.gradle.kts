plugins {
    id("flipper.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
}



kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.provider.api)

        implementation(projects.components.ktx)

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
