plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.bridge.transport.common.api)

        implementation(projects.components.bridge.transport.ble.api)
        implementation(projects.components.bridge.transport.ble.common)
        implementation(projects.components.bridge.transport.ble.http)

        implementation(projects.components.core.log)
        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.cio)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.ble.client)
    }
}
