plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.ksp.gradle)
    implementation(libs.compose.multiplatform.gradle)
    implementation(libs.compose.gradle)
    implementation(libs.vaniktech)
    implementation(libs.klibs.gradle.android)
    implementation(libs.klibs.gradle.convention)

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
