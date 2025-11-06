plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

commonDependencies {
    implementation(projects.components.principal.api)

    implementation(projects.components.di)
    implementation(projects.components.log)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)
}
