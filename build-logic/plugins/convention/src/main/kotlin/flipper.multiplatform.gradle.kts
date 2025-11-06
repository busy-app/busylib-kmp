import com.flipperdevices.buildlogic.ApkConfig
import com.flipperdevices.buildlogic.ApkConfig.DISABLE_NATIVE

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("flipper.lint")
}

kotlin {
    androidLibrary {
        namespace = "com.flipperdevices.${
            project.path
                .removePrefix(":components:")
                .replace(":", ".")
                .replace("-", "")
        }"
        compileSdk = ApkConfig.COMPILE_SDK_VERSION
        minSdk = ApkConfig.MIN_SDK_VERSION
        // todo current version of Jetbrains Compose Resources
        //  doesn't handle new android resourceless plugin
        androidResources.enable = true
    }
    jvm("desktop")

    if (!DISABLE_NATIVE) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withAndroidTarget()
                withJvm()
            }
        }
    }
}

includeCommonKspConfigurationTo("kspAndroid", "kspDesktop")

project.suppressOptIn()
project.ignoreNoDiscoveredTests()

