package com.haesung.watchvoice.watch

import android.app.Application
import android.content.Context
import com.haesung.watchvoice.watch.data.WearableCommandTransport
import com.haesung.watchvoice.watch.domain.CommandTransport
import timber.log.Timber

class WatchVoiceApp : Application() {

    lateinit var container: WatchContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = WatchContainer(this)
    }
}

/**
 * Manual dependency container. Kept deliberately small — a DI framework can replace it later
 * without touching call sites, which resolve dependencies through [Context.watchContainer].
 */
class WatchContainer(context: Context) {
    val commandTransport: CommandTransport = WearableCommandTransport(context.applicationContext)
}

val Context.watchContainer: WatchContainer
    get() = (applicationContext as WatchVoiceApp).container
