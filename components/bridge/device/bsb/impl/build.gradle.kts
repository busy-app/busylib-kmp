plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.device.bsb.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(projects.components.core.buildkonfig)

        implementation(projects.components.bridge.device.common.api)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.info.api)
        implementation(projects.components.bridge.feature.battery.api)
        implementation(projects.components.bridge.feature.wifi.api)
        implementation(projects.components.bridge.feature.ble.api)
        implementation(projects.components.bridge.feature.settings.api)
        implementation(projects.components.bridge.feature.timezone.api)
        implementation(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.api)
        implementation(projects.components.bridge.feature.events.api)
        implementation(projects.components.bridge.feature.oncall.api)
        implementation(projects.components.bridge.feature.smarthome.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
        implementation(projects.components.core.wrapper)
    }
}
