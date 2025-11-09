plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("suppress-optin")
}

kotlin {
    jvm()
    androidLibrary {}
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()
}

includeCommonKspConfigurationTo("kspJvm", "kspAndroid", "kspIosX64", "kspIosArm64", "kspIosSimulatorArm64", "kspMacosX64", "kspMacosArm64")
