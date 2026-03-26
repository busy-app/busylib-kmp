package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiWrappedTypeRuleTest {

    private fun lintInApiModule(
        code: String,
        config: Config = Config.empty
    ): Int = lintWithPath(
        code,
        "components/feature/events/api/src/commonMain/kotlin/Test.kt",
        config = config
    )

    private fun lintWithPath(
        code: String,
        relativePath: String,
        config: Config = Config.empty
    ): Int = ApiWrappedTypeRule(config).lintWithPath(code, relativePath)

    @Test
    fun `reports in feature api module`() {
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `does not report in feature impl module`() {
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/feature/events/impl/src/commonMain/kotlin/Test.kt"
        )
        assertEquals(0, findings)
    }

    @Test
    fun `does not report in non-feature api module`() {
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        val findings = lintWithPath(
            code,
            "components/api/src/commonMain/kotlin/Test.kt"
        )
        assertEquals(0, findings)
    }

    @Test
    fun `reports public function returning Flow`() {
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `reports public function returning StateFlow`() {
        val code = """
            fun getState(): StateFlow<Int> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `reports public function returning SharedFlow`() {
        val code = """
            fun getEvents(): SharedFlow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `reports public function returning Result`() {
        val code = """
            fun doSomething(): Result<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `reports public property returning Flow`() {
        val code = """
            val updates: Flow<String> get() = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `does not report wrapped types`() {
        val code = """
            fun getUpdates(): WrappedFlow<String> = TODO()
            fun getState(): WrappedStateFlow<Int> = TODO()
            fun getEvents(): WrappedSharedFlow<String> = TODO()
            fun doSomething(): CResult<String> = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `does not report regular types`() {
        val code = """
            fun getName(): String = TODO()
            fun getCount(): Int = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `does not report private function returning Flow`() {
        val code = """
            private fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `does not report private property returning StateFlow`() {
        val code = """
            private val state: StateFlow<Int> get() = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `reports suspend function returning WrappedFlow`() {
        val code = """
            suspend fun getUpdates(): WrappedFlow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `does not report non-suspend function returning WrappedFlow`() {
        val code = """
            fun getUpdates(): WrappedFlow<String> = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `reports only forbidden type for suspend function returning raw Flow`() {
        val code = """
            suspend fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `does not report suspend function returning CResult`() {
        val code = """
            suspend fun doSomething(): CResult<String> = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code))
    }

    @Test
    fun `reports generic Flow type`() {
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `reports nullable Flow type`() {
        val code = """
            fun getUpdates(): Flow<String>? = TODO()
        """.trimIndent()
        assertEquals(1, lintInApiModule(code))
    }

    @Test
    fun `does not report for ignored path`() {
        val config = TestConfig(
            "ignoredPaths" to listOf("components/feature/events/api")
        )
        val code = """
            fun getUpdates(): Flow<String> = TODO()
        """.trimIndent()
        assertEquals(0, lintInApiModule(code, config))
    }
}
