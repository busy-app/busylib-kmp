package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

fun <T> Flow<T>?.orEmpty(): Flow<T> = this ?: emptyFlow()

fun <T> Flow<T>.merge(flow: Flow<T>): Flow<T> = listOf(this, flow).merge()
