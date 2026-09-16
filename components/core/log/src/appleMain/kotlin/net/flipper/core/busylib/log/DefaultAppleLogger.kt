package net.flipper.core.busylib.log

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.posix.QOS_CLASS_UTILITY

class DefaultAppleLogger : AppleLogger {
    private val queue = createLogQueue()

    private fun String.escapeForNSLog(): String = replace("%", "%%")

    @OptIn(ExperimentalForeignApi::class)
    private fun createLogQueue(): dispatch_queue_t {
        val attr = dispatch_queue_attr_make_with_qos_class(
            null, // DISPATCH_QUEUE_SERIAL = NULL in Darwin headers
            QOS_CLASS_UTILITY,
            0
        )
        return dispatch_queue_create("net.flipper.busylib.log", attr)
    }
    private fun log(tag: String?, message: String) {
        val line = when (tag) {
            null -> message.escapeForNSLog()
            else -> "[$tag] $message".escapeForNSLog()
        }
        dispatch_async(queue) {
            NSLog(line)
        }
    }

    override fun error(tag: String?, message: String) = log(tag, message)

    override fun info(tag: String?, message: String) = log(tag, message)

    override fun verbose(tag: String?, message: String) = log(tag, message)

    override fun warn(tag: String?, message: String) = log(tag, message)

    override fun debug(tag: String?, message: String) = log(tag, message)

    override fun wtf(tag: String?, message: String) = log(tag, message)
}
