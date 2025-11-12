package net.flipper.core.busylib.log

import platform.Foundation.NSLog

actual inline fun error(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun error(tag: String?, error: Throwable, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun info(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun verbose(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun warn(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun debug(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}

actual inline fun wtf(tag: String?, logMessage: () -> String) {
    if (tag == null) {
        NSLog(logMessage())
    } else {
        NSLog("[$tag] ${logMessage()}")
    }
}
