plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

kotlin.android.namespace = "com.flipperdevices.core.log"

androidDependencies {
    implementation(libs.timber)
}
