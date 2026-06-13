plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.ktor.client.core)
    }
}
