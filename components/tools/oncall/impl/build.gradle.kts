plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.tools.oncall.api)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.log)

        implementation(projects.components.bridge.feature.provider.api)
        implementation(projects.components.bridge.feature.oncall.api)

        implementation(libs.kotlin.coroutines)
    }
}
