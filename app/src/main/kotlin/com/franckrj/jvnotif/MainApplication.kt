package com.franckrj.jvnotif

import android.app.Application
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.NotifsManager

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        System.setProperty("http.keepAlive", "true")
        PrefsManager.initializeSharedPrefs(applicationContext)
        AccountsManager.loadListOfAccounts()
        NotifsManager.initializeNotifs(applicationContext)
    }
}
