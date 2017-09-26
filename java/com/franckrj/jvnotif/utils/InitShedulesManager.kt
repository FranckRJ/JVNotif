package com.franckrj.jvnotif.utils

import android.content.Context
import android.os.Build
import com.franckrj.jvnotif.InitAlarmBootReceiver
import com.franckrj.jvnotif.FetchNotifJobService

object InitShedulesManager {
    fun initSchedulerAndReceiver(context: Context) {
        if (Build.VERSION.SDK_INT < 21) {
            InitAlarmBootReceiver.enableBootReceiverAndInitAlarm(context)
        } else {
            FetchNotifJobService.initJobScheduler(context)
        }
    }
}
