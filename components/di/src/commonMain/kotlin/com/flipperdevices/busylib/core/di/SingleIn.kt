package com.flipperdevices.busylib.core.di

import me.tatarka.inject.annotations.Scope
import kotlin.reflect.KClass

@Scope // <-- kotlin-inject scoping
@Retention(AnnotationRetention.RUNTIME)
annotation class SingleIn(val scope: KClass<*>)