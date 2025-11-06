plugins {
    id("flipper.multiplatform")
    id("flipper.multiplatform-dependencies")
    id("flipper.anvil-multiplatform")
    alias(libs.plugins.kotlinSerialization)
}

commonDependencies {
    implementation(projects.components.bsb.auth.principal.api)

    implementation(projects.components.core.di)
    implementation(projects.components.core.log)

    implementation(projects.components.bsb.preference.api)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.json)
}
