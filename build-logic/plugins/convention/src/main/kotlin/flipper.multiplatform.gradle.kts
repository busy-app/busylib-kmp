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
            macosArm64()
        }
    }
    applyDefaultHierarchyTemplate()
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }
}
