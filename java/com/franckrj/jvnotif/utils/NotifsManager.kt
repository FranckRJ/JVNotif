package com.franckrj.jvnotif.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorInt
import android.support.v4.app.NotificationCompat
import android.support.v4.util.SimpleArrayMap
import com.franckrj.jvnotif.R

/*TODO: Améliorer la personnalisation des notifs (genre le truc de lockscreen etc par ex).*/
object NotifsManager {
    private val listOfNotifType: SimpleArrayMap<NotifTypeInfo.Names, NotifTypeInfo> = SimpleArrayMap()

    fun initializeNotifs(context: Context) {
        val mpNotifType = NotifTypeInfo("com.franckrj.jvnotif.NEW_MP",
                                        4,
                                        context.getString(R.string.mpChannel),
                                        context.getString(R.string.mpChannelDesc),
                                        Color.YELLOW,
                                        1000,
                                        1000,
                                        longArrayOf(0, 200, 200, 200),
                                        (if (Build.VERSION.SDK_INT >= 16) @Suppress("DEPRECATION") Notification.PRIORITY_HIGH else null),
                                        (if (Build.VERSION.SDK_INT >= 26) NotificationManager.IMPORTANCE_HIGH else 0))
        listOfNotifType.put(NotifTypeInfo.Names.MP, mpNotifType)

        if (Build.VERSION.SDK_INT >= 26) {
            val notificationService: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (i in 0 until listOfNotifType.size()) {
                val newNotifType: NotifTypeInfo = listOfNotifType.valueAt(i)
                val newChannel = NotificationChannel(newNotifType.channelID, newNotifType.channelName, newNotifType.importance)

                newChannel.description = newNotifType.channelDesc
                if (newNotifType.lightColor != null) {
                    newChannel.enableLights(true)
                    newChannel.lightColor = (newNotifType.lightColor)
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

    fun pushNotif(notifTypeName: NotifTypeInfo.Names, contentTitle: String, contentText: String, context: Context) {
        val notifType: NotifTypeInfo? = listOfNotifType.get(notifTypeName)

        if (notifType != null) {
            val notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder = NotificationCompat.Builder(context, notifType.channelID)

            notificationBuilder.setSmallIcon(R.mipmap.ic_notif)
            notificationBuilder.setContentTitle(contentTitle)
            notificationBuilder.setContentText(contentText)
            if (notifType.lightColor != null) {
                notificationBuilder.setLights(notifType.lightColor, notifType.lightOnMS, notifType.lightOffMS)
            }
            if (notifType.vibratePattern.isNotEmpty()) {
                notificationBuilder.setVibrate(notifType.vibratePattern)
            }
            if (notifType.priority != null) {
                notificationBuilder.priority = notifType.priority
            }

            notificationService.notify(notifType.notifID, notificationBuilder.build())
        }
    }

    class NotifTypeInfo(val channelID: String = "",
                        val notifID: Int = 0,
                        val channelName: String = "",
                        val channelDesc: String = "",
                        @ColorInt val lightColor: Int? = null,
                        val lightOnMS: Int = 0,
                        val lightOffMS: Int = 0,
                        val vibratePattern: LongArray = LongArray(0),
                        val priority: Int? = null,
                        val importance: Int = 0) {

        enum class Names {
            MP
        }
    }
}
