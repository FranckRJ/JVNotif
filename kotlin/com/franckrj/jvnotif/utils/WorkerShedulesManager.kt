package com.franckrj.jvnotif.utils

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.franckrj.jvnotif.FetchNotifWorker
import java.util.concurrent.TimeUnit

object WorkerShedulesManager {
    fun initSchedulers(resetSchedules: Boolean = false) {
        val fetchNotifConstraints: Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val fetchNotifRequest = PeriodicWorkRequestBuilder<FetchNotifWorker>(PrefsManager.getLong(PrefsManager.LongPref.Names.AUTOCHECK_PERIOD_TIME), TimeUnit.MILLISECONDS)
                                .setConstraints(fetchNotifConstraints)
                                .build()
        WorkManager.getInstance().enqueueUniquePeriodicWork("jvnotif", (if (resetSchedules) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP), fetchNotifRequest)
    }

    fun launchNow() {
        val fetchNotifRequest = OneTimeWorkRequestBuilder<FetchNotifWorker>().build()
        WorkManager.getInstance().enqueue(fetchNotifRequest)
    }
}
