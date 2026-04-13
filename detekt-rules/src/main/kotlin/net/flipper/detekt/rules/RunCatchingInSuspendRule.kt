package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.parents

private const val ISSUE_DESCRIPTION =
    "runCatching/mapCatching in a suspend context silently swallows CancellationException, " +
        "breaking structured concurrency. Use runSuspendCatching/mapSuspendCatching instead."

private val FORBIDDEN_CATCHING_CALLS = setOf("runCatching", "mapCatching")

private val COROUTINE_BUILDER_NAMES = setOf(
    // Coroutine builders
    "launch", "async", "runBlocking",
    // Scope functions
    "withContext", "coroutineScope", "supervisorScope",
    // Timeout
    "withTimeout", "withTimeoutOrNull",
    // Flow builders
    "flow", "channelFlow", "callbackFlow",
    // Flow operators (unambiguous — only exist on Flow with suspend lambda)
    "mapLatest", "flatMapLatest", "transformLatest",
    "collectLatest"
)

/**
 * Reports usages of `runCatching` and `mapCatching` inside suspend functions
 * or coroutine builder lambdas.
 *
 * Both functions wrap **all** throwables — including `CancellationException` —
 * inside a `Result.failure`. This prevents coroutine cancellation from propagating,
 * which can cause coroutines to hang or leak.
 *
 * Use `runSuspendCatching` / `mapSuspendCatching` instead — they re-throw
 * `CancellationException` before wrapping.
 */
class RunCatchingInSuspendRule(config: Config) : Rule(config, ISSUE_DESCRIPTION) {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in FORBIDDEN_CATCHING_CALLS) return

        if (isInSuspendContext(expression)) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = ISSUE_DESCRIPTION
                )
            )
        }
    }

    private fun isInSuspendContext(expression: KtCallExpression): Boolean {
        for (parent in expression.parents) {
            when (parent) {
                is KtNamedFunction -> {
                    return parent.hasModifier(KtTokens.SUSPEND_KEYWORD)
                }

                is KtLambdaExpression -> {
                    if (isCoroutineBuilderLambda(parent)) return true
                }

                else -> continue
            }
        }
        return false
    }

    /**
     * Checks if a lambda expression is a trailing lambda or value argument
     * of a known coroutine builder / flow operator.
     */
    private fun isCoroutineBuilderLambda(lambda: KtLambdaExpression): Boolean {
        val parentElement = lambda.parent ?: return false

        val callExpression = when (parentElement) {
            is KtLambdaArgument -> parentElement.parent as? KtCallExpression
            is KtValueArgument -> {
                val argList = parentElement.parent ?: return false
                argList.parent as? KtCallExpression
            }
            else -> null
        } ?: return false

        val calleeName = callExpression.calleeExpression?.text ?: return false
        return calleeName in COROUTINE_BUILDER_NAMES
    }
}
