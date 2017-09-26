package com.franckrj.jvnotif

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class InitAlarmBootReceiver : BroadcastReceiver() {
    companion object {
        fun enableBootReceiverAndInitAlarm(context: Context) {
            val receiver: ComponentName = ComponentName(context, BroadcastReceiver::class.java)
            val packageManager: PackageManager = context.packageManager

            packageManager.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
            FetchNotifService.initAlarm(context)
        }
    }

    /*A ce qu'il parait ces 3 intents sont nécessaire pour être sur que ça marche.*/
    override fun onReceive(context: Context, intent: Intent) {
        if ((intent.action == "android.intent.action.BOOT_COMPLETED" ||
             intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
             intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") && Build.VERSION.SDK_INT < 21) {
            FetchNotifService.initAlarm(context)
        }
    }
}
