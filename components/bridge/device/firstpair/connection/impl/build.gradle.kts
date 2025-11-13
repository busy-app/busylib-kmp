plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
    id("ru.astrainteractive.gradleplugin.android.core")
    
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(projects.components.bridge.device.firstpair.connection.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.log)

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
