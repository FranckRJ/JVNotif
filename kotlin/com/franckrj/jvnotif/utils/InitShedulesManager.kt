package com.franckrj.jvnotif.utils

import android.content.Context
import android.os.Build
import com.franckrj.jvnotif.FetchNotifJobService
import com.franckrj.jvnotif.FetchNotifService

object InitShedulesManager {
    fun initSchedulers(context: Context) {
        if (Build.VERSION.SDK_INT < 21) {
            FetchNotifService.initAlarm(context)
        } else {
            FetchNotifJobService.initJobScheduler(context)
        }
    }
}
