plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("suppress-optin")
    id("flipper.publish")
}

pluginManager.apply(ru.astrainteractive.gradleplugin.plugin.AndroidSdkPlugin::class)
pluginManager.apply(ru.astrainteractive.gradleplugin.plugin.AndroidJavaPlugin::class)
pluginManager.apply(ru.astrainteractive.gradleplugin.plugin.AndroidNamespacePlugin::class)
pluginManager.apply(ru.astrainteractive.gradleplugin.plugin.JavaVersionPlugin::class)

tasks.withType<TestReport>().configureEach {
    enabled = true
}

kotlin {
    jvm()
    androidLibrary {}
    if (appleEnabled) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        if (macOSEnabled) {
            macosX64()
            macosArm64()
        }
    }
    applyDefaultHierarchyTemplate()
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.all {
            //  Workaround for Kotlin/Native interop regression in K/N 2.3.20.
            //  nw_protocol_stack_set_transport_protocol crashes with TypeCastException
            //  because NW protocol options cannot be cast to NSObject.
            //  Tracked in: https://youtrack.jetbrains.com/issue/KT-85508/
            freeCompilerArgs += "-Xbinary=genericSafeCasts=false"
        }
    }
}

var configurations = arrayListOf(
    "kspJvm",
    "kspAndroid",
)

if (appleEnabled) {
    configurations += arrayListOf(
        "kspIosX64",
        "kspIosArm64",
        "kspIosSimulatorArm64"
    )
    if (macOSEnabled) {
        configurations += arrayListOf(
            "kspMacosX64",
            "kspMacosArm64"
        )
    }
}

@Suppress("SpreadOperator")
includeCommonKspConfigurationTo(
    *configurations.toTypedArray()
)
