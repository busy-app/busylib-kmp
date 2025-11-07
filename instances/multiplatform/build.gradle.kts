import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo
import ru.astrainteractive.gradleplugin.property.extension.PrimitivePropertyValueExt.requireInt


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlinx-serialization")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.android.apk.name")
    id("ru.astrainteractive.gradleplugin.android.namespace")
}

kotlin {
    jvm()
    androidTarget()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    applyDefaultHierarchyTemplate()

    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX }
        .forEach {
            it.binaries.framework {
                baseName = "BridgeConnection"
                isStatic = true

                export(libs.decompose)

                export(projects.components.bridge.config.api)
                export(projects.components.bridge.connectionbuilder.api)
                export(projects.components.bridge.device.bsb.api)
                export(projects.components.bridge.device.common.api)
                export(projects.components.bridge.device.firstpair.connection.api)
                export(projects.components.bridge.feature.battery.api)
                export(projects.components.bridge.feature.common.api)
                export(projects.components.bridge.feature.firmwareUpdate.api)
                export(projects.components.bridge.feature.info.api)
                export(projects.components.bridge.feature.link.api)
                export(projects.components.bridge.feature.provider.api)
                export(projects.components.bridge.feature.rpc.api)
                export(projects.components.bridge.feature.screenStreaming.api)
                export(projects.components.bridge.feature.wifi.api)
                export(projects.components.bridge.orchestrator.api)
                export(projects.components.bridge.service.api)

                export(projects.components.bridge.transport.ble.api)
                export(projects.components.bridge.transport.common.api)
                export(projects.components.bridge.transport.mock.api)
                export(projects.components.bridge.transportconfigbuilder.api)
            }
        }
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.log)
        implementation(projects.components.ktx)

        implementation(kotlin.compose.runtime)
        implementation(kotlin.compose.ui)
        implementation(kotlin.compose.foundation)
        implementation(kotlin.compose.material)
        implementation(kotlin.compose.materialIconsExtended)

        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)

        implementation(projects.entrypoint)

        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.serialization.json)
        implementation(libs.decompose)
        implementation(libs.decompose.composeExtension)
        implementation(libs.klibs.kstorage)
    }

    sourceSets.jvmMain.dependencies {
        implementation(libs.decompose.composeExtension)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.timber)
        implementation(libs.ble.client)
        implementation(libs.androidx.activity.compose)
        implementation(libs.appcompat)
    }

    sourceSets.appleMain.dependencies {
        implementation(projects.components.bridge.transport.ble.impl)
    }
}

android {
    defaultConfig {
        namespace = requireProjectInfo.group
        applicationId = requireProjectInfo.group
        versionCode = baseGradleProperty("project.version.code").requireInt
        versionName = requireProjectInfo.versionString

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
        debug {
            isDebuggable = true
        }
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
}
