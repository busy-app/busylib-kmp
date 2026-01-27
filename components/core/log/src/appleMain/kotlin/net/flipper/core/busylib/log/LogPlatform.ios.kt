package net.flipper.core.busylib.log

actual inline fun error(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.error(tag, logMessage())
}

actual inline fun error(tag: String?, error: Throwable, logMessage: () -> String) {
    val message = "${logMessage()} | ${error.message}"
    AppleLoggerDelegate.logger.error(tag, message)
}

actual inline fun info(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.info(tag, logMessage())
}

actual inline fun verbose(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.verbose(tag, logMessage())
}

actual inline fun warn(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.warn(tag, logMessage())
}

actual inline fun debug(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.debug(tag, logMessage())
}

actual inline fun wtf(tag: String?, logMessage: () -> String) {
    AppleLoggerDelegate.logger.wtf(tag, logMessage())
}
