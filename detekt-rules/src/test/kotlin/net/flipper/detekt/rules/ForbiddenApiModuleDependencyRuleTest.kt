package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import dev.detekt.test.utils.compileForTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ForbiddenApiModuleDependencyRuleTest {

    private fun lintWithPath(code: String, relativePath: String, config: Config = Config.empty): Int {
        val rule = ForbiddenApiModuleDependencyRule(config)
        val tempDir = Files.createTempDirectory("detekt-test")
        try {
            val file = tempDir.resolve(relativePath)
            Files.createDirectories(file.parent)
            Files.writeString(file, code)
            val ktFile = compileForTest(file)
            return rule.lint(ktFile).size
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reports forbidden dependency in api module`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/events/api/build.gradle.kts"
        )
        assertEquals(1, findings)
    }

    @Test
    fun `reports api() dependency in api module`() {
        val code = """
            dependencies {
                api(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/ble/api/build.gradle.kts"
        )
        assertEquals(1, findings)
    }

    @Test
    fun `does not report in non-api module - impl`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/events/impl/build.gradle.kts"
        )
        assertEquals(0, findings)
    }

    @Test
    fun `does not report in non-api module - provisioning`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/watchers/provisioning/build.gradle.kts"
        )
        assertEquals(0, findings)
    }

    @Test
    fun `does not report when no forbidden dependency`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.info.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/events/api/build.gradle.kts"
        )
        assertEquals(0, findings)
    }

    @Test
    fun `does not report for ignored path`() {
        val config = TestConfig(
            "ignoredPaths" to listOf("components/bridge/feature/events/api")
        )
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/events/api/build.gradle.kts",
            config
        )
        assertEquals(0, findings)
    }

    @Test
    fun `reports multiple forbidden dependencies`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
                api(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/bridge/feature/new/api/build.gradle.kts"
        )
        assertEquals(2, findings)
    }

    @Test
    fun `does not report in root build file`() {
        val code = """
            dependencies {
                implementation(projects.components.bridge.feature.rpc.api)
            }
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "build.gradle.kts"
        )
        assertEquals(0, findings)
    }
}
