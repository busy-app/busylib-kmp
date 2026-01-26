plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        api(projects.components.bridge.feature.common.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.serialization.json)
    }
}
