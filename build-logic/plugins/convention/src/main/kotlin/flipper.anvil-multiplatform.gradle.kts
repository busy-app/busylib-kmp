plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("dev.zacsweers.metro")
    id("com.google.devtools.ksp")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.metro.utils.annotations)
            }
        }
    }
}

ksp {
    arg("metro.scope", "net.flipper.busylib.core.di.BusyLibGraph")
}

dependencies {
    add("kspJvm", libs.metro.utils.compiler)
    add("kspAndroid", libs.metro.utils.compiler)
}
