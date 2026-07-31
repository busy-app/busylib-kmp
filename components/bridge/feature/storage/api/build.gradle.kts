plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.components.core.ktx)

        api(projects.components.bridge.feature.common.api)

        implementation(libs.kotlin.coroutines)
    }
}
