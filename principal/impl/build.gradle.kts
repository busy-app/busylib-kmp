plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

commonDependencies {
    implementation(projects.principal.api)

    implementation(projects.di)
    implementation(projects.log)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)
}
