package com.kycis.demo

import android.app.Application
import com.kycis.demo.kycis.KycisIntegration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KycDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KycisIntegration.init(this)
    }
}
