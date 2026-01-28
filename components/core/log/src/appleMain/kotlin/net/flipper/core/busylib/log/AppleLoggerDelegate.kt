package net.flipper.core.busylib.log

object AppleLoggerDelegate {
    var logger: AppleLogger = DefaultAppleLogger()
        private set

    fun setup(logger: AppleLogger) {
        this.logger = logger
    }
}
