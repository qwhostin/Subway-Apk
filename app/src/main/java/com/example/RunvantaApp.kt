package com.example

import android.app.Application
import com.example.ads.AdMobManager
import com.example.audio.SoundManager
import com.example.data.local.RunvantaDatabase
import com.example.data.local.RunvantaRepository
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RunvantaApp : Application() {

    lateinit var database: RunvantaDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var repository: RunvantaRepository
        private set
    lateinit var soundManager: SoundManager
        private set
    lateinit var adMobManager: AdMobManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = RunvantaDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = RunvantaRepository(this, database, settingsDataStore)
        soundManager = SoundManager(this)
        adMobManager = AdMobManager(this)

        appScope.launch {
            repository.ensureSeeded()
        }
    }
}
