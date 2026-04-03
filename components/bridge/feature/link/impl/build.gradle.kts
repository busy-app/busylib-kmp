plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.cloud.api)
        implementation(projects.components.cloud.rest.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.principal.api)

        implementation(libs.klibs.kstorage)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.settings)
        implementation(libs.settings.coroutines)
        implementation(libs.settings.observable)
    }
}
