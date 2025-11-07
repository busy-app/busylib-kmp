plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("dev.zacsweers.metro")
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(projects.components.bridge.device.firstpair.connection.api)

        implementation(projects.components.di)
        implementation(projects.components.log)

        implementation(libs.ble.client)
        implementation(libs.appcompat)
    }

    sourceSets.androidUnitTest.dependencies {
        implementation(libs.ble.client.mock)
        implementation(libs.mockk)
        implementation(libs.kotlin.coroutines.test)

        implementation(libs.kotlin.test)
    }
}
