plugins {
    id("flipper.multiplatform")
    id("flipper.anvil-multiplatform")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.components.core.di)

        implementation(libs.kotlin.coroutines)
    }
    sourceSets.androidMain.dependencies {
        implementation(projects.components.core.log)
        implementation(libs.appcompat)
        implementation(libs.androidx.lifecycle)
        implementation(libs.androidx.lifecycle.process)
    }
    androidLibrary {
        withHostTest {}
    }

    sourceSets {
        val androidHostTest by getting
        androidHostTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.mockk)
        }
    }
}
