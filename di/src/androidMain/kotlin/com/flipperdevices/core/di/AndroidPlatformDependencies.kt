package com.flipperdevices.core.di

import android.app.Activity
import android.service.notification.NotificationListenerService
import kotlin.reflect.KClass

class AndroidPlatformDependencies(
    val splashScreenActivity: KClass<out Activity>,
    val notificationListenerService: KClass<out NotificationListenerService>?
) : PlatformDependencies
