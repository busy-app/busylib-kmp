plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.log)
        implementation(projects.components.core.buildkonfig)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)

        // FlipperFileSystem exposes kotlinx-io types in its public signature
        api(libs.kotlin.io)
        implementation(libs.encoding.hash.md)
        implementation(libs.kotlin.serialization.json)
    }

    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlin.coroutines.test)
    }
}
