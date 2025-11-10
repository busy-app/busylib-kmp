plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("suppress-optin")
}

val appleEnabled = project.findProperty("flipper.appleEnabled")?.toString()?.toBoolean() ?: true

kotlin {
    jvm()
    androidLibrary {}
    if (appleEnabled) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
    }

    applyDefaultHierarchyTemplate()
}

var configurations = arrayListOf(
    "kspJvm",
    "kspAndroid",
)

if (appleEnabled) {
    configurations += arrayListOf(
        "kspIosX64",
        "kspIosArm64",
        "kspIosSimulatorArm64",
        "kspMacosX64",
        "kspMacosArm64"
    )
}

includeCommonKspConfigurationTo(
    *configurations.toTypedArray()
)
