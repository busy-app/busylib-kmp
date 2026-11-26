plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.bridge.feature.oncall.api)

        implementation(libs.kotlin.coroutines)
    }
}
