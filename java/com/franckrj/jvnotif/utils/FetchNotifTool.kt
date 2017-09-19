package com.franckrj.jvnotif.utils

import android.content.Context
import android.os.AsyncTask
import android.support.annotation.StringRes
import android.support.v4.util.SimpleArrayMap
import android.widget.Toast
import com.franckrj.jvnotif.R

class FetchNotifTool(val context: Context) {
    private val listOfCurrentRequests: ArrayList<GetNumberOfMPForAccount> = ArrayList()
    private val listOfNumberOfMPPerAccounts: SimpleArrayMap<String, String> = SimpleArrayMap()
    var fetchNotifIsFinishedListener: FetchNotifIsFinished? = null
    var showToasts: Boolean = false

    companion object {
        val wakeLockTimeout: Long = 300000
        val repeatTime: Long = 1800000
        val EXTRA_SHOW_TOAST: String = "EXTRA_SHOW_TOAST"
    }

    private val newNumberOfMPReceivedListener = object : GetNumberOfMPForAccount.NewNumberOfMPReceived {
        override fun onReceiveNewNumberOfMP(nicknameOfAccount: String, numberOfMP: String?, getter: GetNumberOfMPForAccount) {
            listOfCurrentRequests.remove(getter)
            listOfNumberOfMPPerAccounts.put(nicknameOfAccount, numberOfMP ?: "")

            /*Une fois que toutes les requêtes sont terminées on affiche une notif.*/
            if (listOfCurrentRequests.isEmpty()) {
                makeAndPushNotificationForMP()
                fetchNotifIsFinishedListener?.onFetchNotifIsFinished()
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
        text = context.getString(R.string.accountsWithNewMP, text)

        if (totalNumberOfMP > 0) {
            var oldNumberOfMP: Int = 0

            if (PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
                oldNumberOfMP = PrefsManager.getInt(PrefsManager.IntPref.Names.LAST_NUMBER_OF_MP_FETCHED)
            }

            if (totalNumberOfMP > oldNumberOfMP) {
                val title: String = context.getString(R.string.newNumberOfMP, totalNumberOfMP.toString())

                NotifsManager.pushNotif(NotifsManager.NotifTypeInfo.Names.MP, title, text, context)
                PrefsManager.putInt(PrefsManager.IntPref.Names.LAST_NUMBER_OF_MP_FETCHED, totalNumberOfMP)
                PrefsManager.putBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE, true)
                PrefsManager.applyChanges()
            }
        } else {
            if (PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
                NotifsManager.cancelNotif(NotifsManager.NotifTypeInfo.Names.MP, context)
                PrefsManager.putInt(PrefsManager.IntPref.Names.LAST_NUMBER_OF_MP_FETCHED, -1)
                PrefsManager.putBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE, false)
                PrefsManager.applyChanges()
            }

            showShortToast(R.string.noNewMP)
        }
    }

    private fun showShortToast(@StringRes textID: Int) {
        if (showToasts) {
            Toast.makeText(context, textID, Toast.LENGTH_SHORT).show()
        }
    }

    fun startFetchNotif() {
        if (listOfCurrentRequests.isEmpty()) {
            val listOfAccounts: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()

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
    }

    fun stopFetchNotif() {
        val iterator: MutableListIterator<GetNumberOfMPForAccount> = listOfCurrentRequests.listIterator()
        while (iterator.hasNext()) {
            val currentRequest: GetNumberOfMPForAccount = iterator.next()
            currentRequest.numberOfMPListener = null
            currentRequest.cancel(false)
            iterator.remove()
        }
    }

    private class GetNumberOfMPForAccount(val nickname: String, val cookie: String) : AsyncTask<Void, Void, String?>() {
        var numberOfMPListener: NewNumberOfMPReceived? = null

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
            numberOfMPListener?.onReceiveNewNumberOfMP(nickname, result, this)
        }

        interface NewNumberOfMPReceived {
            fun onReceiveNewNumberOfMP(nicknameOfAccount: String, numberOfMP: String?, getter: GetNumberOfMPForAccount)
        }
    }

    interface FetchNotifIsFinished {
        fun onFetchNotifIsFinished()
    }
}
