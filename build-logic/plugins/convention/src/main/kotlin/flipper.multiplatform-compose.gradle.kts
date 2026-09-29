plugins {
    id("flipper.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.resources)
            implementation(libs.jetbrains.compose.preview)
            if (!macOSEnabled) {
                implementation(libs.jetbrains.compose.tooling)
            }
        }
        val jvmMain by getting
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
