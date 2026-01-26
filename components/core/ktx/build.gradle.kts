plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.buildkonfig)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)

        implementation(libs.kotlin.io)
        implementation(libs.encoding.hash.md)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
