plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.wrapper)

        api(projects.components.bridge.feature.common.api)
    }
}
