package net.flipper.core.busylib.log

interface AppleLogger {
    fun error(tag: String?, message: String)
    fun info(tag: String?, message: String)
    fun verbose(tag: String?, message: String)
    fun warn(tag: String?, message: String)
    fun debug(tag: String?, message: String)
    fun wtf(tag: String?, message: String)
}
