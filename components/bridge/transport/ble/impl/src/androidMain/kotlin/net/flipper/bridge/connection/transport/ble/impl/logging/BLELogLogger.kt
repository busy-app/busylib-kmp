package net.flipper.bridge.connection.transport.ble.impl.logging

import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.debug
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import net.flipper.core.busylib.log.warn
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.LegacyAbstractLogger
import org.slf4j.helpers.MessageFormatter

internal class BLELogLogger(loggerName: String) : LegacyAbstractLogger() {
    init {
        name = loggerName
    }

    private val taggedLogger = TaggedLogger(loggerName)

    override fun getFullyQualifiedCallerName(): String? = null

    // Allow all to filter in :core:log module
    override fun isTraceEnabled(): Boolean = true
    override fun isDebugEnabled(): Boolean = true
    override fun isInfoEnabled(): Boolean = true
    override fun isWarnEnabled(): Boolean = true
    override fun isErrorEnabled(): Boolean = true

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?,
    ) {
        val message = MessageFormatter.basicArrayFormat(messagePattern, arguments)
        when (level) {
            Level.TRACE -> taggedLogger.verbose { message.withThrowable(throwable) }
            Level.DEBUG -> taggedLogger.debug { message.withThrowable(throwable) }
            Level.INFO -> taggedLogger.info { message.withThrowable(throwable) }
            Level.WARN -> taggedLogger.warn { message.withThrowable(throwable) }
            Level.ERROR -> taggedLogger.error(throwable) { message }
            null -> Unit
        }
    }

    private fun String.withThrowable(throwable: Throwable?): String =
        if (throwable == null) this else "$this\n${throwable.stackTraceToString()}"
}
