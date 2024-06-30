package com.franckrj.jvnotif.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.collection.SimpleArrayMap
import com.franckrj.jvnotif.MainActivity
import com.franckrj.jvnotif.NotificationDismissedReceiver
import com.franckrj.jvnotif.R

/* TODO: Améliorer la personnalisation des notifs (genre le truc de lockscreen etc par ex). */
/* TODO: La couleur de la notification n'est pas la bonne. */
object NotifsManager {
    const val INVALID_NOTIF_ID: Int = -1
    /* C'est des nombres au pif, c'est débile mais je peux plus vraiment changer ça. */
    const val MP_NOTIF_ID: Int = 4
    const val STARS_NOTIF_ID: Int = 8

    private val listOfNotifType: SimpleArrayMap<Int, NotifTypeInfo> = SimpleArrayMap()

    private fun makeNotif(notifType: NotifTypeInfo, contentTitle: String, contentText: String, context: Context): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, notifType.channelId)

        notificationBuilder.setSmallIcon(notifType.notifIconId)
        notificationBuilder.setContentTitle(contentTitle)
        notificationBuilder.setContentText(contentText)

        if (notifType.lightAndNotifColor != null) {
            notificationBuilder.color = notifType.lightAndNotifColor
            notificationBuilder.setLights(notifType.lightAndNotifColor, notifType.lightOnMS, notifType.lightOffMS)
        }

        if (notifType.vibratePattern.isNotEmpty()) {
            notificationBuilder.setVibrate(notifType.vibratePattern)
        }

        if (notifType.priority != null) {
            notificationBuilder.priority = notifType.priority
        }

        if (notifType.broadcastDismiss) {
            val intent = Intent(context, NotificationDismissedReceiver::class.java)
            intent.putExtra(NotificationDismissedReceiver.EXTRA_NOTIF_ID, notifType.notifId)

            notificationBuilder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE))
        }

        if (notifType.clickToOpenHome) {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra(MainActivity.EXTRA_NOTIF_IS_CANCELED, true)
            intent.putExtra(MainActivity.EXTRA_NOTIF_CANCELED_ID, notifType.notifId)

            notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE))
            notificationBuilder.setAutoCancel(true)
        }

        return notificationBuilder.build()
    }

    fun initializeNotifs(context: Context) {
        val mpNotifType = NotifTypeInfo("com.franckrj.jvnotif.NEW_MP",
                                        MP_NOTIF_ID,
                                        context.getString(R.string.mpChannel),
                                        context.getString(R.string.mpChannelDesc),
                                        R.mipmap.ic_notif_mp,
                                        Color.rgb(255, 93, 53),
                                        1000,
                                        1000,
                                        longArrayOf(0, 200, 200, 200),
                                        (if (Build.VERSION.SDK_INT >= 16) @Suppress("DEPRECATION") Notification.PRIORITY_HIGH else null),
                                        (if (Build.VERSION.SDK_INT >= 26) NotificationManager.IMPORTANCE_HIGH else 0),
                                        true,
                                        true,
                                        PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)
        listOfNotifType.put(mpNotifType.notifId, mpNotifType)

        val starsNotifType = NotifTypeInfo("com.franckrj.jvnotif.NEW_STARS",
                                           STARS_NOTIF_ID,
                                           context.getString(R.string.starsChannel),
                                           context.getString(R.string.starsChannelDesc),
                                           R.mipmap.ic_notif_stars,
                                           Color.rgb(13, 77, 105),
                                           1000,
                                           1000,
                                           longArrayOf(0, 200, 200, 200),
                                           (if (Build.VERSION.SDK_INT >= 16) @Suppress("DEPRECATION") Notification.PRIORITY_HIGH else null),
                                           (if (Build.VERSION.SDK_INT >= 26) NotificationManager.IMPORTANCE_HIGH else 0),
                                           true,
                                           true,
                                           PrefsManager.BoolPref.Names.STARS_NOTIF_IS_VISIBLE)
        listOfNotifType.put(starsNotifType.notifId, starsNotifType)

        if (Build.VERSION.SDK_INT >= 26) {
            val notificationService: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (i: Int in 0 until listOfNotifType.size()) {
                val newNotifType: NotifTypeInfo = listOfNotifType.valueAt(i)
                val newChannel = NotificationChannel(newNotifType.channelId, newNotifType.channelName, newNotifType.importance)

                newChannel.description = newNotifType.channelDesc
                if (newNotifType.lightAndNotifColor != null) {
                    newChannel.enableLights(true)
                    newChannel.lightColor = (newNotifType.lightAndNotifColor)
                } else {
                    newChannel.enableLights(false)
                }
                if (newNotifType.vibratePattern.isNotEmpty()) {
                    newChannel.enableVibration(true)
                    newChannel.vibrationPattern = newNotifType.vibratePattern
                } else {
                    newChannel.enableVibration(false)
                }

                notificationService.createNotificationChannel(newChannel)
            }
        }
    }

    fun getBoolPrefToChangeForNotif(notifTypeId: Int): PrefsManager.BoolPref.Names? = listOfNotifType.get(notifTypeId)?.boolPrefToChange

    fun pushNotifAndUpdateInfos(notifTypeId: Int, contentTitle: String, contentText: String, context: Context) {
        val notifType: NotifTypeInfo? = listOfNotifType.get(notifTypeId)

        if (notifType != null) {
            val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif: Notification = makeNotif(notifType, contentTitle, contentText, context)

            notificationService.notify(notifType.notifId, notif)

            if (notifType.boolPrefToChange != null) {
                PrefsManager.putBool(notifType.boolPrefToChange, true)
                PrefsManager.applyChanges()
            }
        }
    }

    fun cancelNotifAndClearInfos(notifTypeId: Int, context: Context) {
        val notifType: NotifTypeInfo? = listOfNotifType.get(notifTypeId)

        if (notifType != null) {
            val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationService.cancel(notifType.notifId)
            NotificationDismissedReceiver.onNotifDismissed(notifType.notifId)
        }
    }

    class NotifTypeInfo(val channelId: String = "",
                        val notifId: Int = INVALID_NOTIF_ID,
                        val channelName: String = "",
                        val channelDesc: String = "",
                        @DrawableRes val notifIconId: Int = 0,
                        @ColorInt val lightAndNotifColor: Int? = null,
                        val lightOnMS: Int = 0,
                        val lightOffMS: Int = 0,
                        val vibratePattern: LongArray = LongArray(0),
                        val priority: Int? = null,
                        val importance: Int = 0,
                        val broadcastDismiss: Boolean = false,
                        val clickToOpenHome: Boolean = false,
                        val boolPrefToChange: PrefsManager.BoolPref.Names?)
}
