package com.franckrj.jvnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager

class NotificationDismissedReceiver : BroadcastReceiver() {
    companion object {
        val EXTRA_NOTIF_ID: String = "EXTRA_NOTIF_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifID = intent.extras.getInt(EXTRA_NOTIF_ID)

        if (notifID == NotifsManager.MP_NOTIF_ID) {
            PrefsManager.putInt(PrefsManager.IntPref.Names.LAST_NUMBER_OF_MP_FETCHED, -1)
            PrefsManager.putBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE, false)
            PrefsManager.applyChanges()
        }
    }
}
