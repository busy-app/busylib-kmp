package com.flipperdevices.core.busylib.log

import com.flipperdevices.busylib.core.buildkonfig.BuildKonfigBusyBle

@Suppress("PropertyName")
interface LogTagProvider {
    @Suppress("VariableNaming")
    val TAG: String
}

inline fun LogTagProvider.error(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED) {
        error(TAG, logMessage)
    }
}

inline fun LogTagProvider.error(error: Throwable?, logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED) {
        if (error == null) {
            error(TAG, logMessage)
        } else {
            error(TAG, error, logMessage)
        }
    }
}

inline fun LogTagProvider.info(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED) {
        info(TAG, logMessage)
    }
}

inline fun LogTagProvider.warn(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED) {
        warn(TAG, logMessage)
    }
}

inline fun LogTagProvider.wtf(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED) {
        wtf(TAG, logMessage)
    }
}

inline fun LogTagProvider.verbose(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED && BuildKonfigBusyBle.IS_VERBOSE_LOG_ENABLED) {
        verbose(TAG, logMessage)
    }
}

inline fun LogTagProvider.debug(logMessage: () -> String) {
    if (BuildKonfigBusyBle.IS_LOG_ENABLED && BuildKonfigBusyBle.IS_VERBOSE_LOG_ENABLED) {
        debug(TAG, logMessage)
    }
}
