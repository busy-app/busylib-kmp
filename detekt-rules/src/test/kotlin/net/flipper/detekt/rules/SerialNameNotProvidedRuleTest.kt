package net.flipper.detekt.rules

import dev.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class SerialNameNotProvidedRuleTest {

    private fun lint(code: String): Int = SerialNameNotProvidedRule(Config.empty)
        .lintWithPath(code, "Test.kt")

    @Test
    fun `reports serializable without serialname inside constructor`() {
        val code = """
            import kotlinx.serialization.Serializable

            @Serializable
            data class DaoTimerSnapshot(
                val field1: String,
            )
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun `reports serializable without serialname inside body and constructor`() {
        val code = """
            import kotlinx.serialization.Serializable

            @Serializable
            data class DaoTimerSnapshot(
                val field1: String,
            ) {
                val field2: String
            }
        """.trimIndent()

        assertEquals(2, lint(code))
    }

    @Test
    fun `reports serializable enum without serialname`() {
        val code = """
            import kotlinx.serialization.Serializable

            @Serializable
            enum class DaoMusicTheme {
                FOCUS,
                HARDCORE,
                RELAX
            }
        """.trimIndent()

        assertEquals(3, lint(code))
    }

    @Test
    fun `does not report serializable with serialname inside constructor`() {
        val code = """
            import kotlinx.serialization.SerialName
            import kotlinx.serialization.Serializable

            @Serializable
            data class DaoTimerSnapshot(
                @SerialName("snapshot_timestamp_ms")
                val field1: String,
            )
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun `does not report serializable with transient inside constructor`() {
        val code = """
            import kotlinx.serialization.Transient
            import kotlinx.serialization.Serializable

            @Serializable
            data class DaoTimerSnapshot(
                @Transient
                val field1: String = "",
            )
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun `does not report serializable with transient inside body`() {
        val code = """
            import kotlinx.serialization.Transient
            import kotlinx.serialization.Serializable

            @Serializable
            class DaoTimerSnapshot {
                @Transient
                val field1: String = ""
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun `does not report serializable with transient inside enum`() {
        val code = """
            import kotlinx.serialization.Transient
            import kotlinx.serialization.Serializable

            @Serializable
            enum class DaoMusicTheme {
                @Transient
                FOCUS
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }
}
