package com.franckrj.jvnotif.utils

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.collection.SimpleArrayMap
import com.franckrj.jvnotif.MainActivity
import com.franckrj.jvnotif.R

class FetchNotifTool(val context: Context) {
    private val listOfCurrentRequests: ArrayList<GetNumberOfMpAndStarsForAccount> = ArrayList()
    private val listOfNumberOfMpAndStarsPerAccounts: SimpleArrayMap<String, AccountsManager.MpAndStarsNumbers> = SimpleArrayMap()
    var fetchNotifIsFinishedListener: FetchNotifIsFinished? = null

    companion object {
        const val wakeLockTimeout: Long = 120_000
        const val ACTION_FETCH_NOTIF_STATE_CHANGED: String = "ACTION_FETCH_NOTIF_STATE_CHANGED"
        const val EXTRA_NEW_FETCH_NOTIF_STATE: String = "EXTRA_NEW_FETCH_NOTIF_STATE"
        const val EXTRA_FETCH_NOTIF_STATE_REASON: String = "EXTRA_FETCH_NOTIF_STATE_REASON"
        const val FETCH_NOTIF_STATE_INVALID: Int = -1
        const val FETCH_NOTIF_STATE_STARTED: Int = 0
        const val FETCH_NOTIF_STATE_FINISHED: Int = 1
        const val FETCH_NOTIF_REASON_NO_REASON: Int = -1
        const val FETCH_NOTIF_REASON_OK: Int = 0
        const val FETCH_NOTIF_REASON_NO_ACCOUNT: Int = 1
        const val FETCH_NOTIF_REASON_ALREADY_RUNNING: Int = 2
        const val FETCH_NOTIF_REASON_NETWORK_ERROR: Int = 3
    }

    private val newNumberOfMpAndStarsReceivedListener = object : GetNumberOfMpAndStarsForAccount.NewNumberOfMpAndStarsReceived {
        override fun onReceiveNewNumberOfMpAndStars(nicknameOfAccount: String, infoForMpAndStars: AccountsManager.MpAndStarsNumbers, getter: GetNumberOfMpAndStarsForAccount) {
            listOfCurrentRequests.remove(getter)
            listOfNumberOfMpAndStarsPerAccounts.put(nicknameOfAccount, infoForMpAndStars)

            /* Une fois que toutes les requêtes sont terminées on affiche une notif. */
            if (listOfCurrentRequests.isEmpty()) {
                val someMpAndStarsNumberHaveBeenFetched: Boolean = updateMpAndStarsNumberOfAccountsAndShowThingsIfNeeded()
                fetchNotifIsFinishedListener?.onFetchNotifIsFinished()
                broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, (if (someMpAndStarsNumberHaveBeenFetched) FETCH_NOTIF_REASON_OK else FETCH_NOTIF_REASON_NETWORK_ERROR))
            }
        }
    }

    /* Retourne true si certains mp ou stars ont pu être récupérés, false sinon. */
    private fun updateMpAndStarsNumberOfAccountsAndShowThingsIfNeeded(): Boolean {
        var someMpAndStarsNumberHaveBeenFetched: Boolean = false

        AccountsManager.clearNumberOfMpAndStarsForAllAccounts()
        for (i: Int in 0 until listOfNumberOfMpAndStarsPerAccounts.size()) {
            val currentMpAndStarsInfos: AccountsManager.MpAndStarsNumbers = listOfNumberOfMpAndStarsPerAccounts.valueAt(i)

            if (!currentMpAndStarsInfos.thereIsANetworkError()) {
                someMpAndStarsNumberHaveBeenFetched = true
            }

            AccountsManager.setNumberOfMpAndStars(listOfNumberOfMpAndStarsPerAccounts.keyAt(i), currentMpAndStarsInfos)
        }

        /* ------------ MP */

        if (AccountsManager.thereIsNoMp()) {
            /* Aucun mp non lu. */
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.MP_NOTIF_ID, context)
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
                val text: String = context.getString(R.string.accountsWithNewMpOrStars, AccountsManager.getAllNicknamesThatHaveMp())

                NotifsManager.pushNotifAndUpdateInfos(NotifsManager.MP_NOTIF_ID, title, text, context)
            }
        }/* else {
            Il y a des mp non lu mais ils correspondent à la notification déjà affichée.
        }*/

        AccountsManager.saveNumberOfMp()

        /* ------------ STARS */

        if (AccountsManager.thereIsNoStars()) {
            /* Aucune star non vue. */
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.STARS_NOTIF_ID, context)
        } else if (AccountsManager.thereIsNewStarsSinceLastSavedInfos() ||
                   !PrefsManager.getBool(PrefsManager.BoolPref.Names.STARS_NOTIF_IS_VISIBLE)) {
            /* Nouvelles stars non vues ou même nombre qu'avant (et supérieur à 0)
             * mais comme la notif a été effacée on l'affiche de nouveau. */
            if (!MainActivity.isTheActiveActivity) {
                val totalNumberOfStars: Int = AccountsManager.getNumberOfStarsForAllAccounts()
                val title: String = if (totalNumberOfStars == 1) {
                    context.getString(R.string.newNumberOfStarsSingular)
                } else {
                    context.getString(R.string.newNumberOfStarsPlural, totalNumberOfStars.toString())
                }
                val text: String = context.getString(R.string.accountsWithNewMpOrStars, AccountsManager.getAllNicknamesThatHaveStars())

                NotifsManager.pushNotifAndUpdateInfos(NotifsManager.STARS_NOTIF_ID, title, text, context)
            }
        }/* else {
            Il y a des stars non vues mais elles correspondent à la notification déjà affichée.
        }*/

        AccountsManager.saveNumberOfStars()

        return someMpAndStarsNumberHaveBeenFetched
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

            listOfNumberOfMpAndStarsPerAccounts.clear()

            if (listOfAccounts.isNotEmpty()) {
                for (account in listOfAccounts) {
                    val newRequest = GetNumberOfMpAndStarsForAccount(account.nickname, account.cookie)

                    listOfCurrentRequests.add(newRequest)
                    newRequest.numberOfMpAndStarsListener = newNumberOfMpAndStarsReceivedListener
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
        val iterator: MutableListIterator<GetNumberOfMpAndStarsForAccount> = listOfCurrentRequests.listIterator()
        while (iterator.hasNext()) {
            val currentRequest: GetNumberOfMpAndStarsForAccount = iterator.next()
            currentRequest.numberOfMpAndStarsListener = null
            currentRequest.cancel(false)
            iterator.remove()
        }
    }

    private class GetNumberOfMpAndStarsForAccount(val nickname: String, val cookie: String) : AsyncTask<Void, Void, AccountsManager.MpAndStarsNumbers>() {
        var numberOfMpAndStarsListener: NewNumberOfMpAndStarsReceived? = null

        override fun doInBackground(vararg param: Void?): AccountsManager.MpAndStarsNumbers {
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
            if (pageContent != null && pageContent.isNotEmpty()) {
                return JVCParser.getMpAndStarsNumbersFromPage(pageContent)
            } else {
                return AccountsManager.MpAndStarsNumbers(AccountsManager.MpAndStarsNumbers.NETWORK_ERROR, AccountsManager.MpAndStarsNumbers.NETWORK_ERROR)
            }
        }

        override fun onPostExecute(result: AccountsManager.MpAndStarsNumbers) {
            super.onPostExecute(result)
            numberOfMpAndStarsListener?.onReceiveNewNumberOfMpAndStars(nickname, result, this)
        }

        interface NewNumberOfMpAndStarsReceived {
            fun onReceiveNewNumberOfMpAndStars(nicknameOfAccount: String, infoForMpAndStars: AccountsManager.MpAndStarsNumbers, getter: GetNumberOfMpAndStarsForAccount)
        }
    }

    interface FetchNotifIsFinished {
        fun onFetchNotifIsFinished()
    }
}
