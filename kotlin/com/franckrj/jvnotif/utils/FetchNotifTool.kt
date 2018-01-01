package com.franckrj.jvnotif.utils

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.util.SimpleArrayMap
import com.franckrj.jvnotif.MainActivity
import com.franckrj.jvnotif.R

class FetchNotifTool(val context: Context) {
    private val listOfCurrentRequests: ArrayList<GetNumberOfMpForAccount> = ArrayList()
    private val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, String> = SimpleArrayMap()
    var fetchNotifIsFinishedListener: FetchNotifIsFinished? = null

    companion object {
        val wakeLockTimeout: Long = 120_000
        val ACTION_FETCH_NOTIF_STATE_CHANGED: String = "ACTION_FETCH_NOTIF_STATE_CHANGED"
        val EXTRA_NEW_FETCH_NOTIF_STATE: String = "EXTRA_NEW_FETCH_NOTIF_STATE"
        val EXTRA_FETCH_NOTIF_STATE_REASON: String = "EXTRA_FETCH_NOTIF_STATE_REASON"
        val FETCH_NOTIF_STATE_INVALID: Int = -1
        val FETCH_NOTIF_STATE_STARTED: Int = 0
        val FETCH_NOTIF_STATE_FINISHED: Int = 1
        val FETCH_NOTIF_REASON_NO_REASON: Int = -1
        val FETCH_NOTIF_REASON_OK: Int = 0
        val FETCH_NOTIF_REASON_NO_ACCOUNT: Int = 1
        val FETCH_NOTIF_REASON_ALREADY_RUNNING: Int = 2
        val FETCH_NOTIF_REASON_ERROR: Int = 3
    }

    private val newNumberOfMpReceivedListener = object : GetNumberOfMpForAccount.NewNumberOfMpReceived {
        override fun onReceiveNewNumberOfMp(nicknameOfAccount: String, numberOfMp: String?, getter: GetNumberOfMpForAccount) {
            listOfCurrentRequests.remove(getter)
            listOfNumberOfMpPerAccounts.put(nicknameOfAccount, numberOfMp ?: "")

            /* Une fois que toutes les requêtes sont terminées on affiche une notif. */
            if (listOfCurrentRequests.isEmpty()) {
                val someMpNumberHaveBeenFetched: Boolean = updateMpNumberOfAccountsAndShowThingsIfNeeded()
                fetchNotifIsFinishedListener?.onFetchNotifIsFinished()
                broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, (if (someMpNumberHaveBeenFetched) FETCH_NOTIF_REASON_OK else FETCH_NOTIF_REASON_ERROR))
            }
        }
    }

    /* Retourne true si certains mp ont pu être récupérés, false sinon. */
    private fun updateMpNumberOfAccountsAndShowThingsIfNeeded(): Boolean {
        var someMpNumberHaveBeenFetched: Boolean = false

        AccountsManager.clearNumberOfMpForAllAccounts()
        for (i: Int in 0 until listOfNumberOfMpPerAccounts.size()) {
            val currentNumberOfMp: Int = (listOfNumberOfMpPerAccounts.valueAt(i).toIntOrNull() ?: -1)

            if (currentNumberOfMp >= 0) {
                someMpNumberHaveBeenFetched = true
                AccountsManager.setNumberOfMp(listOfNumberOfMpPerAccounts.keyAt(i), currentNumberOfMp)
            } else {
                AccountsManager.setNumberOfMp(listOfNumberOfMpPerAccounts.keyAt(i), 0)
            }
        }

        if (AccountsManager.thereIsNoMp()) {
            /* Aucun mp non lu. */
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, context)
        } else if (AccountsManager.thereIsNewMpSinceLastSavedInfos() ||
                   !PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
            /* Nouveaux mp non lu ou même nombre qu'avant (et supérieur à 0)
             * mais comme la notif a été effacée on l'affiche de nouveau. */
            if (!MainActivity.isTheActiveActivity) {
                val totalNumberOfMp: Int = AccountsManager.getNumberOfMpForAllAccounts()
                val title: String = if (totalNumberOfMp == 1) {
                    context.getString(R.string.newNumberOfMpSingular)
                } else {
                    context.getString(R.string.newNumberOfMpPlural, totalNumberOfMp.toString())
                }
                val text: String = context.getString(R.string.accountsWithNewMp, AccountsManager.getAllNicknamesThatHaveMp())

                NotifsManager.pushNotifAndUpdateInfos(NotifsManager.NotifTypeInfo.Names.MP, title, text, context)
            }
        }/* else {
            Il y a des mp non lu mais ils correspondent à la notification déjà affichée.
        }*/

        AccountsManager.saveNumberOfMp()

        return someMpNumberHaveBeenFetched
    }

    private fun broadcastCurrentFetchState(newFetchState: Int, reasonForState: Int = FETCH_NOTIF_REASON_NO_REASON) {
        val fetchNotifStateChangedIntent = Intent(ACTION_FETCH_NOTIF_STATE_CHANGED)

        fetchNotifStateChangedIntent.putExtra(EXTRA_NEW_FETCH_NOTIF_STATE, newFetchState)
        fetchNotifStateChangedIntent.putExtra(EXTRA_FETCH_NOTIF_STATE_REASON, reasonForState)

        LocalBroadcastManager.getInstance(context).sendBroadcast(fetchNotifStateChangedIntent)
    }

    fun startFetchNotif() {
        broadcastCurrentFetchState(FETCH_NOTIF_STATE_STARTED)

        if (listOfCurrentRequests.isEmpty()) {
            val listOfAccounts: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()

            listOfNumberOfMpPerAccounts.clear()

            if (listOfAccounts.isNotEmpty()) {
                for (account in listOfAccounts) {
                    val newRequest = GetNumberOfMpForAccount(account.nickname, account.cookie)

                    listOfCurrentRequests.add(newRequest)
                    newRequest.numberOfMpListener = newNumberOfMpReceivedListener
                    newRequest.execute()
                }
            } else {
                broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, FETCH_NOTIF_REASON_NO_ACCOUNT)
            }
        } else {
            broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, FETCH_NOTIF_REASON_ALREADY_RUNNING)
        }
    }

    fun stopFetchNotif() {
        val iterator: MutableListIterator<GetNumberOfMpForAccount> = listOfCurrentRequests.listIterator()
        while (iterator.hasNext()) {
            val currentRequest: GetNumberOfMpForAccount = iterator.next()
            currentRequest.numberOfMpListener = null
            currentRequest.cancel(false)
            iterator.remove()
        }
    }

    private class GetNumberOfMpForAccount(val nickname: String, val cookie: String) : AsyncTask<Void, Void, String?>() {
        var numberOfMpListener: NewNumberOfMpReceived? = null

        override fun doInBackground(vararg param: Void?): String? {
            val currentWebInfos = WebManager.WebInfos()
            var pageContent: String?
            var numberOfTrysRemaining: Int = 2

            currentWebInfos.followRedirects = false
            currentWebInfos.useBiggerTimeoutTime = false

            do {
                /* TODO: Check pour changer le lien de la requête (si besoin). */
                pageContent = WebManager.sendRequest("http://www.jeuxvideo.com/sso/settings.php", "GET", "", cookie, currentWebInfos)
                numberOfTrysRemaining -= 1
                currentWebInfos.useBiggerTimeoutTime = true
            } while (pageContent.isNullOrEmpty() && numberOfTrysRemaining > 0)

            @Suppress("LiftReturnOrAssignment")
            if (pageContent != null) {
                return JVCParser.getNumberOfMpFromPage(pageContent)
            } else {
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            numberOfMpListener?.onReceiveNewNumberOfMp(nickname, result, this)
        }

        interface NewNumberOfMpReceived {
            fun onReceiveNewNumberOfMp(nicknameOfAccount: String, numberOfMp: String?, getter: GetNumberOfMpForAccount)
        }
    }

    interface FetchNotifIsFinished {
        fun onFetchNotifIsFinished()
    }
}
