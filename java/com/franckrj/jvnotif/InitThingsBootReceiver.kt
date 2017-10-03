package com.franckrj.jvnotif

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import com.franckrj.jvnotif.utils.NotifsManager

class InitThingsBootReceiver : BroadcastReceiver() {
    /*A ce qu'il parait ces 3 intents sont nécessaire pour être sur que ça marche.*/
    override fun onReceive(context: Context, intent: Intent) {
        if ((intent.action == "android.intent.action.BOOT_COMPLETED" ||
             intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
             intent.action == "com.htc.intent.action.QUICKBOOT_POWERON")) {
            NotificationDismissedReceiver.onNotifDismissed(NotifsManager.MP_NOTIF_ID)

            if (Build.VERSION.SDK_INT < 21) {
                FetchNotifService.initAlarm(context)
            }
        }
    }
}
