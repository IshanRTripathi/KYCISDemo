package com.kycis.sdk.core

import android.app.Activity
import android.app.AlertDialog
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidUiBridge {
    private var currentActivityRef: WeakReference<Activity>? = null

    fun updateCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    fun getCurrentActivity(): Activity? = currentActivityRef?.get()

    fun confirm(text: ConfirmUiText): Boolean {
        val activity = currentActivityRef?.get() ?: return false
        if (activity.isFinishing) return false

        val approved = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            AlertDialog.Builder(activity)
                .setTitle(text.title)
                .setPositiveButton(text.startCta) { dialog, _ ->
                    approved.set(true)
                    dialog.dismiss()
                    latch.countDown()
                }
                .setNegativeButton(text.dismissCta) { dialog, _ ->
                    approved.set(false)
                    dialog.dismiss()
                    latch.countDown()
                }
                .setOnCancelListener {
                    approved.set(false)
                    latch.countDown()
                }
                .show()
        }
        latch.await(12, TimeUnit.SECONDS)
        return approved.get()
    }
}
