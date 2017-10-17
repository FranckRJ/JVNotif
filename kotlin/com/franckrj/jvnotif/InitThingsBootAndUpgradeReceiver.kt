package com.franckrj.jvnotif

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.franckrj.jvnotif.utils.InitShedulesManager
import com.franckrj.jvnotif.utils.NotifsManager

class InitThingsBootAndUpgradeReceiver : BroadcastReceiver() {
    /* A ce qu'il parait ces 3 premiers intents sont nécessaire pour être sur que ça marche. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED" ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, context)

            InitShedulesManager.initSchedulers(context)
        }
    }
}
