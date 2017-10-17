package com.franckrj.jvnotif

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.franckrj.jvnotif.utils.FetchNotifTool

class FetchNotifService : Service() {
    private var wakelock: PowerManager.WakeLock? = null
    private val toolForFetchNotif: FetchNotifTool = FetchNotifTool(this)

    companion object {
        fun initAlarm(context: Context) {
            val alarmMgr: AlarmManager = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            val alarmIntent: PendingIntent = PendingIntent.getService(context,
                                                                      0,
                                                                      Intent(context, FetchNotifService::class.java),
                                                                      0)

            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                         SystemClock.elapsedRealtime(),
                                         FetchNotifTool.repeatTime,
                                         alarmIntent)
        }
    }

    private val fetchNotifIsFinishedListener = object : FetchNotifTool.FetchNotifIsFinished {
        override fun onFetchNotifIsFinished() {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager: PowerManager = (getSystemService(Context.POWER_SERVICE) as PowerManager)
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FetchNotifService")
        wakelock?.acquire(FetchNotifTool.wakeLockTimeout)
        toolForFetchNotif.fetchNotifIsFinishedListener = fetchNotifIsFinishedListener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        toolForFetchNotif.showToasts =
                (intent?.getBooleanExtra(FetchNotifTool.EXTRA_SHOW_TOAST, false) ?: false)
        toolForFetchNotif.onlyUpdateAndDontShowNotif =
                (intent?.getBooleanExtra(FetchNotifTool.EXTRA_ONLY_UPDATE_AND_DONT_SHOW_NOTIF, false) ?: false)

        toolForFetchNotif.startFetchNotif()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        toolForFetchNotif.fetchNotifIsFinishedListener = null
        toolForFetchNotif.stopFetchNotif()
        wakelock?.release()
        super.onDestroy()
    }
}
