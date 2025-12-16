import net.flipper.Config.CURRENT_FLAVOR_TYPE
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

    alias(libs.plugins.skie)
}

kotlin {

    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)
        api(projects.components.core.wrapper)
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
        api(projects.components.bridge.feature.ble.api)
        implementation(projects.components.bridge.feature.ble.impl)
        api(projects.components.bridge.feature.settings.api)
        implementation(projects.components.bridge.feature.settings.impl)
        api(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.orchestrator.impl)
        api(projects.components.bridge.service.api)
        implementation(projects.components.bridge.service.impl)
        api(projects.components.bridge.feature.events.api)
        implementation(projects.components.bridge.feature.events.impl)
        api(projects.components.bridge.feature.oncall.api)
        implementation(projects.components.bridge.feature.oncall.impl)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.tcp.cloud.api)
        if (CURRENT_FLAVOR_TYPE.isMockEnabled) {
            implementation(projects.components.bridge.transport.mock.impl)
        }
        if (CURRENT_FLAVOR_TYPE.isCloudEnabled) {
            implementation(projects.components.bridge.transport.tcp.cloud.impl)
        }
        implementation(projects.components.bridge.transport.tcp.lan.api)
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
    sourceSets.jvmMain.dependencies {
        implementation(projects.components.bridge.transport.tcp.lan.impl)
    }
}

kotlin {
    val xcFramework = XCFramework("BusyLibKMP")
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter {
            it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX
        }
        .forEach { target ->
            target.binaries.framework {
                baseName = "BusyLibKMP"
                isStatic = false

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
                export(projects.components.bridge.feature.settings.api)
                export(projects.components.bridge.orchestrator.api)
                export(projects.components.bridge.service.api)

                xcFramework.add(this)
            }
        }

    sourceSets.appleMain {
        dependencies {
            implementation(projects.components.bridge.config.impl)
        }
        if (CURRENT_FLAVOR_TYPE.isMockEnabled) {
            kotlin.srcDir("src/appleMainMock/kotlin")
        }
    }
    sourceSets.iosMain.dependencies {
        implementation(projects.components.bridge.transport.ble.impl)
    }
    sourceSets.macosMain.dependencies {
        implementation(projects.components.bridge.transport.tcp.lan.impl)
    }
}

val zipXCFrameworkDebug by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Creates a ZIP archive of the debug XCFramework"

    dependsOn("assembleBusyLibKMPDebugXCFramework")

    from(layout.buildDirectory.dir("XCFrameworks/debug"))
    include("BusyLibKMP.xcframework/**")
    archiveFileName.set("BusyLibKMP-debug.xcframework.zip")
    destinationDirectory.set(layout.buildDirectory.dir("xcframework-zips"))
}

val zipXCFrameworkRelease by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Creates a ZIP archive of the release XCFramework"

    dependsOn("assembleBusyLibKMPReleaseXCFramework")

    from(layout.buildDirectory.dir("XCFrameworks/release"))
    include("BusyLibKMP.xcframework/**")
    archiveFileName.set("BusyLibKMP-release.xcframework.zip")
    destinationDirectory.set(layout.buildDirectory.dir("xcframework-zips"))
}

// Configure publishing to include the XCFramework zip files
publishing {
    publications {
        // Add XCFramework debug zip as an artifact
        create<MavenPublication>("xcframeworkDebug") {
            artifactId = "${project.name}-xcframework-debug"

            artifact(zipXCFrameworkDebug.get())
        }

        // Add XCFramework release zip as an artifact
        create<MavenPublication>("xcframeworkRelease") {
            artifactId = "${project.name}-xcframework-release"

            artifact(zipXCFrameworkRelease.get())
        }
    }
}
