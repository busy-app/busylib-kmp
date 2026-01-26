import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo
import ru.astrainteractive.gradleplugin.property.extension.PrimitivePropertyValueExt.requireInt


plugins {
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlinx-serialization")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.klibs.gradle.android.sdk)
    alias(libs.plugins.klibs.gradle.android.java)
    alias(libs.plugins.klibs.gradle.android.compose)
    alias(libs.plugins.klibs.gradle.android.namespace)
    alias(libs.plugins.klibs.gradle.java.version)
}
dependencies {
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.ui)
    implementation(libs.jetbrains.compose.resources)
    implementation(libs.jetbrains.compose.preview)
}

dependencies {
    implementation(projects.components.core.log)
    implementation(projects.components.core.ktx)
    implementation(projects.components.core.wrapper)

    api(projects.entrypoint)
    implementation(projects.components.bridge.config.impl)

    implementation(libs.settings)
    implementation(libs.settings.observable)
    implementation(libs.settings.coroutines)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.serialization.json)
    api(libs.decompose)
    implementation(libs.decompose.composeExtension)
    implementation(libs.klibs.kstorage)

    implementation(libs.timber)
    implementation(libs.ble.client)
    implementation(libs.androidx.activity.compose)
    implementation(libs.appcompat)

    implementation(projects.sample.cmp)
}

android {
    defaultConfig {
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
    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }
}
