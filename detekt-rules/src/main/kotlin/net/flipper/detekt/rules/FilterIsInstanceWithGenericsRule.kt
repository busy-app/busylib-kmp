package net.flipper.detekt.rules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeProjection

private const val ISSUE_DESCRIPTION =
    "filterIsInstance<T>() with a parameterized generic type argument loses " +
        "the inner type check due to type erasure, which can lead to runtime failure." +
        "Use filterIsInstance with a star projection " +
        "followed by .filter { it.property is T } instead."

class FilterIsInstanceWithGenericsRule(config: Config) : Rule(config, ISSUE_DESCRIPTION) {
    private class ExpressionFinderKtTreeVisitorVoid : KtTreeVisitorVoid() {
        private var _isFound = false
        val isFound: Boolean
            get() = _isFound

        override fun visitIsExpression(expression: KtIsExpression) {
            _isFound = true
            super.visitIsExpression(expression)
        }
    }

    private fun containsIsExpression(filterCall: KtCallExpression): Boolean {
        // Lambda can be either inside valueArguments or a trailing lambda
        val lambdas = mutableListOf<KtLambdaExpression>()

        filterCall
            .lambdaArguments
            .mapNotNullTo(lambdas) { ktLambdaArgument ->
                ktLambdaArgument.getLambdaExpression()
            }

        filterCall.valueArguments
            .map { ktValueArgument -> ktValueArgument.getArgumentExpression() }
            .filterIsInstance<KtLambdaExpression>()
            .toCollection(lambdas)

        return lambdas.any { lambda ->
            val visitor = ExpressionFinderKtTreeVisitorVoid()
            lambda.bodyExpression?.acceptChildren(visitor)
            visitor.isFound
        }
    }

    /**
     * Checks if the filterIsInstance call is preceded by a `.filter { ... is ... }` call
     * in a dot-qualified chain, which indicates the developer has already added a runtime type check
     */
    private fun isPrecededByIsFilterCall(expression: KtCallExpression): Boolean {
        // The filterIsInstance call is the selector of a dot-qualified expression
        val dotExpr = expression.parent as? KtDotQualifiedExpression ?: return false
        val receiver = dotExpr.receiverExpression

        // The receiver should be another dot-qualified expression ending with .filter { ... }
        val receiverDotExpr = receiver as? KtDotQualifiedExpression ?: return false
        val filterCall = receiverDotExpr.selectorExpression as? KtCallExpression ?: return false

        if (filterCall.calleeExpression?.text != "filter") return false

        // Check the lambda body for an `is` expression
        return containsIsExpression(filterCall)
    }

    /**
     * Checks if the type argument contains generic parameters that are NOT star projections
     * 
     * `Wrapper<T>` -> true, `Wrapper<*>` -> false, `Event.A` -> false
     */
    private fun hasNonStarGenericArgument(typeProjection: KtTypeProjection): Boolean {
        val typeRef = typeProjection.typeReference ?: return false
        val typeElement = typeRef.typeElement ?: return false
        val innerArguments = typeElement.typeArgumentsAsTypes
        if (innerArguments.isEmpty()) return false
        return innerArguments.any { ktTypeReference ->
            ktTypeReference != null && ktTypeReference.text != "*"
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "filterIsInstance") return

        val firstTypeArg = expression.typeArguments.firstOrNull() ?: return

        if (!hasNonStarGenericArgument(firstTypeArg)) return

        // Check if preceded by a .filter { ... is ... } call
        if (isPrecededByIsFilterCall(expression)) return

        report(
            Finding(
                entity = Entity.from(expression),
                message = ISSUE_DESCRIPTION
            )
        )
    }
}
