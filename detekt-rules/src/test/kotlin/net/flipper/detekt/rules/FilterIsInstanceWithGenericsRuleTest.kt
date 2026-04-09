package net.flipper.detekt.rules

import dev.detekt.api.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterIsInstanceWithGenericsRuleTest {

    private fun lint(code: String): Int = FilterIsInstanceWithGenericsRule(Config.empty)
        .lintWithPath(code, "Test.kt")

    @Test
    fun GIVEN_parameterized_generic_type_WHEN_filterIsInstance_THEN_reports() {
        val code = """
            import kotlinx.coroutines.flow.flowOf
            import kotlinx.coroutines.flow.filterIsInstance

            sealed interface Event {
                data class A(val x: Int) : Event
                data class B(val y: Int) : Event
            }
            sealed interface Wrapper<out T : Event> {
                data class Wrapped<out T : Event>(val event: T) : Wrapper<T>
                data object Empty : Wrapper<Nothing>
            }

            inline fun <reified T : Event> get() = flowOf(
                Wrapper.Empty,
                Wrapper.Wrapped(Event.A(1)),
                Wrapper.Wrapped(Event.B(2))
            ).filterIsInstance<Wrapper.Wrapped<T>>()
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_star_projection_WHEN_filterIsInstance_THEN_does_not_report() {
        val code = """
            import kotlinx.coroutines.flow.flowOf
            import kotlinx.coroutines.flow.filterIsInstance

            sealed interface Event {
                data class A(val x: Int) : Event
            }
            sealed interface Wrapper<out T : Event> {
                data class Wrapped<out T : Event>(val event: T) : Wrapper<T>
                data object Empty : Wrapper<Nothing>
            }

            fun get() = flowOf(
                Wrapper.Empty,
                Wrapper.Wrapped(Event.A(1))
            ).filterIsInstance<Wrapper.Wrapped<*>>()
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_no_generics_WHEN_filterIsInstance_THEN_does_not_report() {
        val code = """
            import kotlinx.coroutines.flow.flowOf
            import kotlinx.coroutines.flow.filterIsInstance

            sealed interface Event {
                data class A(val x: Int) : Event
                data class B(val y: Int) : Event
            }

            fun get() = flowOf(
                Event.A(1),
                Event.B(2)
            ).filterIsInstance<Event.A>()
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_list_with_parameterized_generic_WHEN_filterIsInstance_THEN_reports() {
        val code = """
            sealed interface Container<out T> {
                data class Item<out T>(val value: T) : Container<T>
            }

            inline fun <reified T> filterItems(items: List<Any>): List<Container.Item<T>> {
                return items.filterIsInstance<Container.Item<T>>()
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }

    @Test
    fun GIVEN_list_with_star_projection_WHEN_filterIsInstance_THEN_does_not_report() {
        val code = """
            sealed interface Container<out T> {
                data class Item<out T>(val value: T) : Container<T>
            }

            fun filterItems(items: List<Any>): List<Container.Item<*>> {
                return items.filterIsInstance<Container.Item<*>>()
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_preceded_by_filter_with_is_check_WHEN_filterIsInstance_THEN_does_not_report() {
        val code = """
            sealed interface FeatureStatus<out T> {
                data class Supported<out T>(val featureApi: T) : FeatureStatus<T>
            }
            interface RpcFeatureApi

            fun example(items: List<Any>) {
                items
                    .filterIsInstance<FeatureStatus.Supported<*>>()
                    .filter { it.featureApi is RpcFeatureApi }
                    .filterIsInstance<FeatureStatus.Supported<RpcFeatureApi>>()
                    .first()
            }
        """.trimIndent()

        assertEquals(0, lint(code))
    }

    @Test
    fun GIVEN_preceded_by_filter_without_is_check_WHEN_filterIsInstance_with_generic_THEN_reports() {
        val code = """
            sealed interface FeatureStatus<out T> {
                data class Supported<out T>(val featureApi: T) : FeatureStatus<T>
            }
            interface RpcFeatureApi

            fun example(items: List<Any>) {
                items
                    .filter { it != null }
                    .filterIsInstance<FeatureStatus.Supported<RpcFeatureApi>>()
            }
        """.trimIndent()

        assertEquals(1, lint(code))
    }
}
