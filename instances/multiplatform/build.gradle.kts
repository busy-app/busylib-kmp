import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo
import ru.astrainteractive.gradleplugin.property.extension.PrimitivePropertyValueExt.requireInt


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.zacsweers.metro")
    id("kotlinx-serialization")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.android.apk.name")
    id("ru.astrainteractive.gradleplugin.android.namespace")
}

kotlin {
    jvm()
    androidTarget()

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.commonMain.dependencies {

        implementation(projects.components.di)
        implementation(projects.components.ktx)
        implementation(projects.components.log)
        implementation(projects.components.principal.api)
        implementation(projects.components.principal.impl)

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

        implementation(projects.components.bridge.config.api)
        implementation(projects.components.bridge.config.impl)
        implementation(projects.components.bridge.connectionbuilder.api)
        implementation(projects.components.bridge.connectionbuilder.impl)
        implementation(projects.components.bridge.device.bsb.api)
        implementation(projects.components.bridge.device.bsb.impl)
        implementation(projects.components.bridge.device.common.api)
        implementation(projects.components.bridge.device.firstpair.connection.api)
        implementation(projects.components.bridge.device.firstpair.connection.impl)
        implementation(projects.components.bridge.feature.battery.api)
        implementation(projects.components.bridge.feature.battery.impl)
        implementation(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.api)
        implementation(projects.components.bridge.feature.firmwareUpdate.impl)
        implementation(projects.components.bridge.feature.info.api)
        implementation(projects.components.bridge.feature.info.impl)
        implementation(projects.components.bridge.feature.link.api)
        implementation(projects.components.bridge.feature.link.impl)
        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.provider.impl)
        implementation(projects.components.bridge.feature.rpc.api)
        implementation(projects.components.bridge.feature.rpc.impl)
        implementation(projects.components.bridge.feature.screenStreaming.api)
        implementation(projects.components.bridge.feature.screenStreaming.impl)
        implementation(projects.components.bridge.feature.sync.impl)
        implementation(projects.components.bridge.feature.wifi.api)
        implementation(projects.components.bridge.feature.wifi.impl)
        implementation(projects.components.bridge.orchestrator.api)
        implementation(projects.components.bridge.orchestrator.impl)
        implementation(projects.components.bridge.service.api)
        implementation(projects.components.bridge.service.impl)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.common.api)
        implementation(projects.components.bridge.transport.common.impl)
        implementation(projects.components.bridge.transport.mock.api)
        implementation(projects.components.bridge.transport.mock.impl)
        implementation(projects.components.bridge.transportconfigbuilder.api)
        implementation(projects.components.bridge.transportconfigbuilder.impl)

        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.decompose)
        implementation(libs.decompose.composeExtension)
    }

    sourceSets.jvmMain.dependencies {
        implementation(libs.decompose.composeExtension)
    }

    sourceSets.androidMain.dependencies {
        implementation(projects.components.bridge.transport.ble.impl)
        implementation(libs.timber)
        implementation(libs.ble.client)
        implementation(libs.androidx.activity.compose)
        implementation(libs.appcompat)
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
