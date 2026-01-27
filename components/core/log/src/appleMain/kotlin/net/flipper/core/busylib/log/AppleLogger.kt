package net.flipper.core.busylib.log

import platform.Foundation.NSLog

private fun String.escapeForNSLog(): String = replace("%", "%%")

interface AppleLogger {
    fun error(tag: String?, message: String)
    fun info(tag: String?, message: String)
    fun verbose(tag: String?, message: String)
    fun warn(tag: String?, message: String)
    fun debug(tag: String?, message: String)
    fun wtf(tag: String?, message: String)

    companion object {
        fun default(): AppleLogger {
            return object : AppleLogger {
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
        }
    }
}
