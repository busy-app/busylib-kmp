import ru.astrainteractive.gradleplugin.property.baseGradleProperty
import ru.astrainteractive.gradleplugin.property.extension.AndroidModelPropertyValueExt.requireAndroidSdkInfo
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.hierarchyGroup
import ru.astrainteractive.gradleplugin.property.extension.ModelPropertyValueExt.requireProjectInfo
import ru.astrainteractive.gradleplugin.property.extension.PrimitivePropertyValueExt.requireInt


plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.astrainteractive.gradleplugin.java.core")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.zacsweers.metro")
    id("kotlinx-serialization")
    id("ru.astrainteractive.gradleplugin.android.core")
    id("ru.astrainteractive.gradleplugin.android.namespace")
}

kotlin {
    jvm()
    androidTarget()

    applyDefaultHierarchyTemplate()
}
dependencies {
    implementation(projects.components.di)
    implementation(projects.components.ktx)
    implementation(projects.components.log)
    implementation(projects.components.principal.api)
    implementation(projects.components.principal.impl)

    implementation(projects.components.bridge.config.api)
    implementation(projects.components.bridge.config.impl)
    implementation(projects.components.bridge.connectionbuilder.api)
    implementation(projects.components.bridge.connectionbuilder.impl)
    implementation(projects.components.bridge.device.bsb.api)
    implementation(projects.components.bridge.device.bsb.impl)
    implementation(projects.components.bridge.device.common.api)
    implementation(projects.components.bridge.device.firstpair.connection.api)
    implementation(projects.components.bridge.device.firstpair.connection.impl)
    implementation(projects.components.bridge.feature.battery.api)
    implementation(projects.components.bridge.feature.battery.impl)
    implementation(projects.components.bridge.feature.common.api)
    implementation(projects.components.bridge.feature.firmwareUpdate.api)
    implementation(projects.components.bridge.feature.firmwareUpdate.impl)
    implementation(projects.components.bridge.feature.info.api)
    implementation(projects.components.bridge.feature.info.impl)
    implementation(projects.components.bridge.feature.link.api)
    implementation(projects.components.bridge.feature.link.impl)
    implementation(projects.components.bridge.feature.provider.api)
    implementation(projects.components.bridge.feature.provider.impl)
    implementation(projects.components.bridge.feature.rpc.api)
    implementation(projects.components.bridge.feature.rpc.impl)
    implementation(projects.components.bridge.feature.screenStreaming.api)
    implementation(projects.components.bridge.feature.screenStreaming.impl)
    implementation(projects.components.bridge.feature.sync.impl)
    implementation(projects.components.bridge.feature.wifi.api)
    implementation(projects.components.bridge.feature.wifi.impl)
    implementation(projects.components.bridge.orchestrator.api)
    implementation(projects.components.bridge.orchestrator.impl)
    implementation(projects.components.bridge.service.api)
    implementation(projects.components.bridge.service.impl)

    implementation(projects.components.bridge.transport.ble.api)
    implementation(projects.components.bridge.transport.ble.impl)
    implementation(projects.components.bridge.transport.common.api)
    implementation(projects.components.bridge.transport.common.impl)
    implementation(projects.components.bridge.transport.mock.api)
    implementation(projects.components.bridge.transport.mock.impl)
    implementation(projects.components.bridge.transportconfigbuilder.api)
    implementation(projects.components.bridge.transportconfigbuilder.impl)

}


tasks.register<Jar>("fatJar") {

    exclude("META-INF/*.kotlin_module")

    archiveBaseName.set("${requireProjectInfo.name}-${requireProjectInfo.versionString}.jar")

    val jvmTarget = kotlin.targets.getByName("jvm")
    val jvmCompilation = jvmTarget.compilations.getByName("main")
    from(jvmCompilation.output)

    val kspJvmGeneratedDir = layout.buildDirectory.dir("generated/ksp/jvm/jvmMain")
    from(kspJvmGeneratedDir)

    from(
        {
            rootProject.subprojects
                .filter { it != project }
                .flatMap { subproj ->
                    val subDir =
                        subproj.layout.buildDirectory.dir("classes/kotlin/jvm/main").get().asFile
                    if (subDir.exists()) listOf(subDir) else emptyList()
                }
        }
    )

    dependsOn(jvmCompilation.compileKotlinTaskName)
}
