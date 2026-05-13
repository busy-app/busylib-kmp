plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.data)
        implementation(projects.components.core.log)

        api(projects.components.bridge.feature.common.api)
        api(projects.components.bridge.feature.settings.api)

        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)
    }
}
