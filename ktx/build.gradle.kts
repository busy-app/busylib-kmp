plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
}

commonDependencies {
    implementation(projects.log)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.datetime)

    implementation(libs.kotlin.io)
    implementation(libs.encoding.hash.md)
}

androidDependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.browser)
}

commonTestDependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlin.coroutines.test)
}
