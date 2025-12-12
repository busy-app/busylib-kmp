import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.AndroidModelPropertyValueExt.requireAndroidSdkInfo
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
    alias(libs.plugins.skie)
}

kotlin {
    jvm()
    androidTarget()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    macosArm64()
    macosX64()

    applyDefaultHierarchyTemplate()

    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX }
        .forEach { target ->
            target.binaries.framework {
                baseName = "BridgeConnection"
                isStatic = true

                export(libs.decompose)
                export(projects.entrypoint)
            }
        }
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

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

        api(projects.entrypoint)

        implementation(libs.settings)
        implementation(libs.settings.observable)
        implementation(libs.settings.coroutines)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.kotlin.serialization.json)
        api(libs.decompose)
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
    sourceSets.jvmMain.dependencies {
        implementation(compose.desktop.currentOs)
    }
}

android {
    defaultConfig {
        namespace = requireProjectInfo.group
        applicationId = requireProjectInfo.group
        versionCode = baseGradleProperty("project.version.code").requireInt
        versionName = requireProjectInfo.versionString
        compileSdk = requireAndroidSdkInfo.compile
        targetSdk = requireAndroidSdkInfo.target
        minSdk = requireAndroidSdkInfo.min

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

compose.desktop {
    application {
        mainClass = "net.flipper.bridge.connection.AppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BUSYLib"
            packageVersion = "1.0.0"
        }
    }
}
