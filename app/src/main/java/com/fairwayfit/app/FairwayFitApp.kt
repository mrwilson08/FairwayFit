package com.fairwayfit.app

import android.app.Application
import android.content.Context
import com.fairwayfit.app.data.Repository
import com.fairwayfit.app.data.SettingsStore
import com.fairwayfit.app.data.db.AppDatabase
import com.fairwayfit.app.data.health.HealthConnectManager
import com.fairwayfit.app.data.pebble.PebbleManager
import com.fairwayfit.app.data.sync.SyncManager
import com.fairwayfit.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container: small app, so no DI framework. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = AppDatabase.create(appContext)
    val settings = SettingsStore(appContext)
    val sync = SyncManager(appContext, db)
    val repository = Repository(db, sync, appScope)
    val health = HealthConnectManager(appContext)
    val pebble = PebbleManager(appContext, repository, settings, appScope)
}

class FairwayFitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
        container.appScope.launch {
            val current = container.settings.current()
            ReminderScheduler.apply(this@FairwayFitApp, current)
            container.pebble.syncSessionPin(current)
            container.sync.syncQuietly()
        }
    }
}
