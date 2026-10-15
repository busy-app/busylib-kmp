plugins {
    id("flipper.multiplatform")
    id("flipper.metro-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.tools.debug.api)
        implementation(projects.components.bridge.feature.finishSetup.impl)

        implementation(projects.components.core.di)
        implementation(projects.components.core.ktx)
        implementation(projects.components.core.wrapper)

        implementation(libs.klibs.kstorage)
    }
}
