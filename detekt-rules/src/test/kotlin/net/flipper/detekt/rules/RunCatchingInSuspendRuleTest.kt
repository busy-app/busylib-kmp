package net.flipper.detekt.rules

import dev.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class RunCatchingInSuspendRuleTest {

    private fun lint(code: String): Int = RunCatchingInSuspendRule(Config.empty)
        .lintWithPath(code, "Test.kt")

    @Test
    fun GIVEN_runCatching_in_suspend_fun_WHEN_lint_THEN_reports() {
        val code = """
            suspend fun doWork(): Result<Unit> = runCatching {
                println("work")
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_non_suspend_fun_WHEN_lint_THEN_does_not_report() {
        val code = """
            fun doWork(): Result<Unit> = runCatching {
                println("work")
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_launch_WHEN_lint_THEN_reports() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.launch

            fun doWork(scope: CoroutineScope) {
                scope.launch {
                    runCatching { println("work") }
                }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_async_WHEN_lint_THEN_reports() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.async

            fun doWork(scope: CoroutineScope) {
                scope.async {
                    runCatching { println("work") }
                }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_withContext_WHEN_lint_THEN_reports() {
        val code = """
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext

            suspend fun doWork() {
                withContext(Dispatchers.IO) {
                    runCatching { println("work") }
                }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_flow_builder_WHEN_lint_THEN_reports() {
        val code = """
            import kotlinx.coroutines.flow.flow

            fun doWork() = flow {
                runCatching { emit(1) }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runCatching_nested_in_lambda_inside_suspend_fun_WHEN_lint_THEN_reports() {
        val code = """
            suspend fun doWork() {
                listOf(1, 2, 3).forEach {
                    runCatching { println(it) }
                }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_runSuspendCatching_in_suspend_fun_WHEN_lint_THEN_does_not_report() {
        val code = """
            suspend fun doWork(): Result<Unit> = runSuspendCatching {
                println("work")
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_non_suspend_lambda_of_non_suspend_fun_WHEN_lint_THEN_does_not_report() {
        val code = """
            fun doWork(): List<Result<Int>> {
                return listOf(1, 2, 3).map {
                    runCatching { it + 1 }
                }
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_multiple_runCatching_in_suspend_fun_WHEN_lint_THEN_reports_all() {
        val code = """
            suspend fun doWork() {
                runCatching { println("a") }
                runCatching { println("b") }
            }
        """.trimIndent()

        assertEquals(2, lint(code))
    }

    @Test
    fun GIVEN_runCatching_in_mapLatest_WHEN_lint_THEN_reports() {
        val code = """
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.mapLatest

            fun doWork(flow: Flow<Int>) = flow.mapLatest {
                runCatching { it + 1 }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_mapCatching_in_suspend_fun_WHEN_lint_THEN_reports() {
        val code = """
            suspend fun doWork(result: Result<Int>): Result<String> {
                return result.mapCatching { it.toString() }
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_mapCatching_in_non_suspend_fun_WHEN_lint_THEN_does_not_report() {
        val code = """
            fun doWork(result: Result<Int>): Result<String> {
                return result.mapCatching { it.toString() }
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_mapSuspendCatching_in_suspend_fun_WHEN_lint_THEN_does_not_report() {
        val code = """
            suspend fun doWork(result: Result<Int>): Result<String> {
                return result.mapSuspendCatching { it.toString() }
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }
}
