plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)

        api(projects.components.bridge.feature.common.api)
        implementation(projects.components.bridge.feature.rpc.api)

        implementation(libs.kotlin.coroutines)
    }
}
