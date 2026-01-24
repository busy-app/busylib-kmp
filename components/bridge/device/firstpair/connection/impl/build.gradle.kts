plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
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
