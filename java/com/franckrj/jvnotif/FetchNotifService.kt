package com.franckrj.jvnotif

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.annotation.StringRes
import android.support.v4.util.SimpleArrayMap
import android.widget.Toast
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.JVCParser
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.WebManager

/*TODO: Le faire fonctionner sur Android 8.0, avec JobService tout ça.*/
class FetchNotifService : Service() {
    private var firstTimeLaunched: Boolean = true
    private var showToasts: Boolean = false
    private val listOfCurrentRequests: ArrayList<GetNumberOfMPForAccount> = ArrayList()
    private val listOfNumberOfMPPerAccounts: SimpleArrayMap<String, String> = SimpleArrayMap()
    private var wakelock: PowerManager.WakeLock? = null

    companion object {
        private val wakeLockTimeout: Long = 300000

        val EXTRA_SHOW_TOAST: String = "EXTRA_SHOW_TOAST"

        fun initAlarm(context: Context) {
            val intent: Intent = Intent(context, FetchNotifService::class.java)
            val alarmMgr: AlarmManager = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            val alarmIntent: PendingIntent = PendingIntent.getService(context, 0, intent, 0)

            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent)
        }
    }

    private val newNumberOfMPReceivedListener = object : GetNumberOfMPForAccount.NewNumberOfMPListener {
        override fun newNumberOfMPReceived(nicknameOfAccount: String, numberOfMP: String?, getter: GetNumberOfMPForAccount) {
            listOfCurrentRequests.remove(getter)
            listOfNumberOfMPPerAccounts.put(nicknameOfAccount, numberOfMP ?: "")

            /*Une fois que toutes les requêtes sont terminées on affiche une notif.*/
            if (listOfCurrentRequests.isEmpty()) {
                makeAndPushNotificationForMP()
                stopSelf()
            }
        }
    }

    private fun makeAndPushNotificationForMP() {
        var totalNumberOfMP: Int = 0
        var text: String = ""

        for (i in 0 until listOfNumberOfMPPerAccounts.size()) {
            val currentNumberOfMP: Int = (listOfNumberOfMPPerAccounts.valueAt(i).toIntOrNull() ?: 0)

            totalNumberOfMP += currentNumberOfMP
            if (currentNumberOfMP > 0) {
                text += listOfNumberOfMPPerAccounts.keyAt(i) + ", "
            }
        }

        text = text.removeSuffix(", ")
        text = getString(R.string.accountsWithNewMP, text)

        if (totalNumberOfMP > 0) {
            val title: String = getString(R.string.newNumberOfMP, totalNumberOfMP.toString())

            NotifsManager.pushNotif(NotifsManager.NotifTypeInfo.Names.MP, title, text, this)
        } else {
            showShortToast(R.string.noNewMP)
        }
    }

    private fun showShortToast(@StringRes textID: Int) {
        if (showToasts) {
            Toast.makeText(this, textID, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager: PowerManager = (getSystemService(Context.POWER_SERVICE) as PowerManager)
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FetchNotifService")
        wakelock?.acquire(wakeLockTimeout)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showToasts = (intent?.getBooleanExtra(EXTRA_SHOW_TOAST, false) ?: false)

        if (listOfCurrentRequests.isEmpty() && firstTimeLaunched) {
            val listOfAccounts: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()

            firstTimeLaunched = false
            listOfNumberOfMPPerAccounts.clear()

            if (listOfAccounts.isNotEmpty()) {
                for (account in listOfAccounts) {
                    val newRequest = GetNumberOfMPForAccount(account.nickname, account.cookie)

                    listOfCurrentRequests.add(newRequest)
                    newRequest.numberOfMPListener = newNumberOfMPReceivedListener
                    newRequest.execute()
                }
            } else {
                showShortToast(R.string.connectToZeroAccount)
            }
        } else {
            showShortToast(R.string.errorActionAlreadyRunning)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        val iterator: MutableListIterator<GetNumberOfMPForAccount> = listOfCurrentRequests.listIterator()
        while (iterator.hasNext()) {
            val currentRequest: GetNumberOfMPForAccount = iterator.next()
            currentRequest.numberOfMPListener = null
            currentRequest.cancel(false)
            iterator.remove()
        }
        wakelock?.release()
        super.onDestroy()
    }

    private class GetNumberOfMPForAccount(val nickname: String, val cookie: String) : AsyncTask<Void, Void, String?>() {
        var numberOfMPListener: NewNumberOfMPListener? = null

        override fun doInBackground(vararg p0: Void?): String? {
            val currentWebInfos = WebManager.WebInfos()
            val pageContent: String?

            currentWebInfos.followRedirects = false
            /*TODO: Check pour changer le lien de la requête (si besoin).*/
            pageContent = WebManager.sendRequest("http://www.jeuxvideo.com/sso/settings.php", "GET", "", cookie, currentWebInfos)

            @Suppress("LiftReturnOrAssignment")
            if (pageContent != null) {
                return JVCParser.getNumberOfMPFromPage(pageContent)
            } else {
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            numberOfMPListener?.newNumberOfMPReceived(nickname, result, this)
        }

        interface NewNumberOfMPListener {
            fun newNumberOfMPReceived(nicknameOfAccount: String, numberOfMP: String?, getter: GetNumberOfMPForAccount)
        }
    }
}
