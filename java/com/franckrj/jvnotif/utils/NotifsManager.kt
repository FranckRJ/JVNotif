package com.franckrj.jvnotif.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorInt
import android.support.v4.app.NotificationCompat
import android.support.v4.util.SimpleArrayMap
import com.franckrj.jvnotif.NotificationDismissedReceiver
import com.franckrj.jvnotif.R

/*TODO: Améliorer la personnalisation des notifs (genre le truc de lockscreen etc par ex).*/
/*TODO: La couleur de la notification n'est pas la bonne.*/
object NotifsManager {
    private val listOfNotifType: SimpleArrayMap<NotifTypeInfo.Names, NotifTypeInfo> = SimpleArrayMap()

    val MP_NOTIF_ID: Int = 4

    fun initializeNotifs(context: Context) {
        val mpNotifType = NotifTypeInfo("com.franckrj.jvnotif.NEW_MP",
                                        MP_NOTIF_ID,
                                        context.getString(R.string.mpChannel),
                                        context.getString(R.string.mpChannelDesc),
                                        Color.rgb(255, 140, 0),
                                        1000,
                                        1000,
                                        longArrayOf(0, 200, 200, 200),
                                        (if (Build.VERSION.SDK_INT >= 16) @Suppress("DEPRECATION") Notification.PRIORITY_HIGH else null),
                                        (if (Build.VERSION.SDK_INT >= 26) NotificationManager.IMPORTANCE_HIGH else 0),
                                        true)
        listOfNotifType.put(NotifTypeInfo.Names.MP, mpNotifType)

        if (Build.VERSION.SDK_INT >= 26) {
            val notificationService: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (i in 0 until listOfNotifType.size()) {
                val newNotifType: NotifTypeInfo = listOfNotifType.valueAt(i)
                val newChannel = NotificationChannel(newNotifType.channelID, newNotifType.channelName, newNotifType.importance)

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

    fun makeNotif(notifType: NotifTypeInfo, contentTitle: String, contentText: String, context: Context): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, notifType.channelID)

        notificationBuilder.setSmallIcon(R.drawable.ic_notif)
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
            intent.putExtra(NotificationDismissedReceiver.EXTRA_NOTIF_ID, notifType.notifID)

            notificationBuilder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, intent, 0))
        }

        return notificationBuilder.build()
    }

    fun pushNotif(notifTypeName: NotifTypeInfo.Names, contentTitle: String, contentText: String, context: Context) {
        val notifType: NotifTypeInfo? = listOfNotifType.get(notifTypeName)

        if (notifType != null) {
            val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif: Notification = makeNotif(notifType, contentTitle, contentText, context)

            notificationService.notify(notifType.notifID, notif)
        }
    }

    fun cancelNotif(notifTypeName: NotifTypeInfo.Names, context: Context) {
        val notifType: NotifTypeInfo? = listOfNotifType.get(notifTypeName)

        if (notifType != null) {
            val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationService.cancel(notifType.notifID)
        }
    }

    class NotifTypeInfo(val channelID: String = "",
                        val notifID: Int = 0,
                        val channelName: String = "",
                        val channelDesc: String = "",
                        @ColorInt val lightAndNotifColor: Int? = null,
                        val lightOnMS: Int = 0,
                        val lightOffMS: Int = 0,
                        val vibratePattern: LongArray = LongArray(0),
                        val priority: Int? = null,
                        val importance: Int = 0,
                        val broadcastDismiss: Boolean = false) {

        enum class Names {
            MP
        }
    }
}
