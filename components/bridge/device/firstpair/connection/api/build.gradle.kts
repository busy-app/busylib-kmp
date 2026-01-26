plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.ktx)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.immutable)
        implementation(libs.ble.client)

        implementation(libs.appcompat)
    }
}
