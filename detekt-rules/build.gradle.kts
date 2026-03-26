plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.jupiter)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
