package com.franckrj.jvnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager

class NotificationDismissedReceiver : BroadcastReceiver() {
    companion object {
        val EXTRA_NOTIF_ID: String = "EXTRA_NOTIF_ID"

        fun onNotifDismissed(notifId: Int) {
            val boolPrefToChange: PrefsManager.BoolPref.Names? = NotifsManager.getBoolPrefToChangeForNotif(notifId)

            if (boolPrefToChange != null) {
                PrefsManager.putBool(boolPrefToChange, false)
                PrefsManager.applyChanges()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        onNotifDismissed(intent.getIntExtra(EXTRA_NOTIF_ID, NotifsManager.INVALID_NOTIF_ID))
    }
}
