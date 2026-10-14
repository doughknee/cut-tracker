package com.relentnet.cuttracker

import android.app.Application
import android.content.Context
import com.relentnet.cuttracker.data.CutDb
import com.relentnet.cuttracker.data.Repo
import com.relentnet.cuttracker.data.SettingsStore

/** Plain singletons; the app is small enough that DI would be ceremony. */
class App : Application() {
    val db: CutDb by lazy { CutDb.build(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val repo: Repo by lazy { Repo(this, db, settings) }

    companion object {
        fun of(context: Context): App = context.applicationContext as App
    }
}
