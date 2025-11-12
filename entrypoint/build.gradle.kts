import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.target.Family

plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.publication")
    alias(libs.plugins.skie)
}

kotlin {
    val xcFramework = XCFramework("BusyLibKMP")
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family == Family.IOS }
        .forEach { target ->
            target.binaries.framework {
                baseName = "BusyLibKMP"
                isStatic = true

                binaryOption("bundleId", "net.flipper.busylib")

                export(projects.components.principal.api)
                export(projects.components.cloud.api)
                export(projects.components.bridge.config.api)
                export(projects.components.bridge.device.common.api)
                export(projects.components.bridge.feature.battery.api)
                export(projects.components.bridge.feature.firmwareUpdate.api)
                export(projects.components.bridge.feature.info.api)
                export(projects.components.bridge.feature.link.api)
                export(projects.components.bridge.feature.provider.api)
                export(projects.components.bridge.feature.screenStreaming.api)
                export(projects.components.bridge.feature.wifi.api)
                export(projects.components.bridge.orchestrator.api)
                export(projects.components.bridge.service.api)
                export(projects.components.bridge.transport.ble.api)

                xcFramework.add(this)
            }
        }

    sourceSets.commonMain.dependencies {
        implementation(projects.components.di)
        implementation(projects.components.ktx)
        implementation(projects.components.log)
        api(projects.components.principal.api)
        api(projects.components.cloud.api)

        api(projects.components.bridge.config.api)
        implementation(projects.components.bridge.connectionbuilder.api)
        implementation(projects.components.bridge.connectionbuilder.impl)
        implementation(projects.components.bridge.device.bsb.api)
        implementation(projects.components.bridge.device.bsb.impl)
        api(projects.components.bridge.device.common.api)
        api(projects.components.bridge.feature.battery.api)
        implementation(projects.components.bridge.feature.battery.impl)
        implementation(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.firmwareUpdate.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.impl)
        api(projects.components.bridge.feature.info.api)
        implementation(projects.components.bridge.feature.info.impl)
        api(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.link.impl)
        api(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.provider.impl)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.rpc.impl)
        api(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.screenStreaming.impl)
        implementation(projects.components.bridge.feature.sync.impl)
        api(projects.components.bridge.feature.wifi.api)
        implementation(projects.components.bridge.feature.wifi.impl)
        api(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.orchestrator.impl)
        api(projects.components.bridge.service.api)
        implementation(projects.components.bridge.service.impl)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.mock.impl)
        implementation(projects.components.bridge.transportconfigbuilder.api)
        implementation(projects.components.bridge.transportconfigbuilder.impl)
        implementation(libs.kotlin.coroutines)
        implementation(libs.ktor.client.core)
    }
    sourceSets.androidMain.dependencies {
        api(projects.components.bridge.device.firstpair.connection.api)
        implementation(projects.components.bridge.device.firstpair.connection.impl)
        implementation(projects.components.bridge.transport.ble.impl)
        implementation(libs.ble.client)
    }
    sourceSets.appleMain.dependencies {
        api(projects.components.bridge.transport.ble.impl)
        api(projects.components.bridge.config.impl)
    }
}
