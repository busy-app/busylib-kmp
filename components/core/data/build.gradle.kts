plugins {
    id("flipper.multiplatform")
    id("kotlinx-serialization")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.di)
        implementation(projects.components.core.log)
        implementation(libs.kotlin.serialization.json)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}
