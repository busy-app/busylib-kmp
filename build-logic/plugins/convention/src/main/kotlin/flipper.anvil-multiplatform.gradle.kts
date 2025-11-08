plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.google.devtools.ksp")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.inject.runtime)
                implementation(libs.kotlin.inject.kimchi.runtime)
            }
        }
    }
}

dependencies {
    add("commonKsp", libs.kotlin.inject.compiler)
    add("commonKsp", libs.kotlin.inject.kimchi.compiler)
}
