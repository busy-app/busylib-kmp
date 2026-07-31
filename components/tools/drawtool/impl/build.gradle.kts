plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.tools.drawtool.api)

        implementation(projects.components.core.data)
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        implementation(projects.components.core.wrapper)

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.drawTool.api)
        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.storage.api)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.watchers.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.io)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.klibs.kstorage)
        implementation(libs.settings)
        implementation(libs.settings.coroutines)
        implementation(libs.kotlinx.crypto)
        implementation(libs.kotlinx.crypto.provider)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
        implementation(libs.settings.test)
    }
}
