package com.kycis.sdk.core

import android.app.Activity
import android.app.Application
import android.os.Bundle

internal class LifecycleTracker(
    private val runtime: SdkRuntime,
) : Application.ActivityLifecycleCallbacks {
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        runtime.bindCurrentActivity(activity)
        runtime.onScreenObserved(activity.javaClass.simpleName)
        runtime.onUserInteractionObserved()
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
