package com.flipperdevices.core.busylib.ktx.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun <T> Flow<T>?.orEmpty(): Flow<T> = this ?: emptyFlow()
