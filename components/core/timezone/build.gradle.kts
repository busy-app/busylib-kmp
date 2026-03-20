plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
    }
}
