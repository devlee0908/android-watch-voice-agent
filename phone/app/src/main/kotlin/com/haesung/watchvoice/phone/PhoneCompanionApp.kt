package com.haesung.watchvoice.phone

import android.app.Application
import timber.log.Timber

class PhoneCompanionApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
