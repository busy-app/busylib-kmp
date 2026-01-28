package net.flipper.core.busylib.log

import platform.Foundation.NSLog

class DefaultAppleLogger: AppleLogger {
    private fun String.escapeForNSLog(): String = replace("%", "%%")

    override fun error(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }

    override fun info(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }

    override fun verbose(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }

    override fun warn(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }

    override fun debug(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }

    override fun wtf(tag: String?, message: String) {
        if (tag == null) {
            NSLog(message.escapeForNSLog())
        } else {
            NSLog("[$tag] $message".escapeForNSLog())
        }
    }
}
