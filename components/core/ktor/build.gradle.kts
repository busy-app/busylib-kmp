plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {

            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
