package com.relentnet.cuttracker

import android.app.Application
import android.content.Context
import com.relentnet.cuttracker.data.CutDb
import com.relentnet.cuttracker.data.Repo
import com.relentnet.cuttracker.data.SettingsStore
import com.relentnet.cuttracker.notify.Reminders
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Plain singletons; the app is small enough that DI would be ceremony. */
class App : Application() {
    val db: CutDb by lazy { CutDb.build(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val repo: Repo by lazy { Repo(this, db, settings) }

    override fun onCreate() {
        super.onCreate()
        Reminders.ensureChannel(this)
        repo.scope.launch {
            settings.flow.map { Triple(it.onboarded, it.weighInTime, it.pickTime) }.distinctUntilChanged().collect { (onboarded, _, _) ->
                if (onboarded) Reminders.schedule(this@App, settings.current())
            }
        }
    }

    companion object {
        fun of(context: Context): App = context.applicationContext as App
    }
}
