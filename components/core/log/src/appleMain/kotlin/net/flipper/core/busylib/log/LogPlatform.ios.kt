package net.flipper.core.busylib.log

import platform.Foundation.NSLog

fun String.escapeForNSLog(): String = replace("%", "%%")

actual inline fun error(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun error(tag: String?, error: Throwable, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun info(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun verbose(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun warn(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun debug(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}

actual inline fun wtf(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage().escapeForNSLog())
    } else {
        NSLog("[$tag] ${logMessage()}".escapeForNSLog())
    }
}
