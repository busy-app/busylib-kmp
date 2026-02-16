plugins {
    id("flipper.multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.components.core.log)

            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)

            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.logging)
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
