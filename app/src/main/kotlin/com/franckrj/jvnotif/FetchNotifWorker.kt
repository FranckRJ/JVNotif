package com.franckrj.jvnotif

import android.content.Context
import android.content.Intent
import androidx.collection.SimpleArrayMap
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.JVCParser
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.WebManager
import java.util.concurrent.locks.ReentrantLock

class FetchNotifWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    private val listOfNumberOfMpAndStarsPerAccounts: SimpleArrayMap<String, AccountsManager.MpAndStarsNumbers> = SimpleArrayMap()

    companion object {
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

        private val lock: ReentrantLock = ReentrantLock()
    }

    private fun downloadNumberOfMpAndStarsForAccount(cookie: String): AccountsManager.MpAndStarsNumbers {
        val currentWebInfos = WebManager.WebInfos()
        var pageContent: String?
        var numberOfTrysRemaining: Int = 2

        currentWebInfos.followRedirects = false
        currentWebInfos.useBiggerTimeoutTime = false

        do {
            pageContent = WebManager.sendRequest("https://www.jeuxvideo.com/mailform.php", "GET", "", cookie, currentWebInfos)
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

    /* TODO: Gérer au moins un peu le isStopped. */
    override fun doWork(): Result {
        broadcastCurrentFetchState(FETCH_NOTIF_STATE_STARTED)

        if (lock.tryLock()) {
            val listOfAccounts: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()

            listOfNumberOfMpAndStarsPerAccounts.clear()

            if (listOfAccounts.isNotEmpty()) {
                @Suppress("JoinDeclarationAndAssignment")
                val someMpAndStarsNumberHaveBeenFetched: Boolean
                for (account in listOfAccounts) {
                    listOfNumberOfMpAndStarsPerAccounts.put(account.nickname, downloadNumberOfMpAndStarsForAccount(account.cookie))
                }
                someMpAndStarsNumberHaveBeenFetched = updateMpAndStarsNumberOfAccountsAndShowThingsIfNeeded()
                broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, (if (someMpAndStarsNumberHaveBeenFetched) FETCH_NOTIF_REASON_OK else FETCH_NOTIF_REASON_NETWORK_ERROR))
            } else {
                broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, FETCH_NOTIF_REASON_NO_ACCOUNT)
            }
            lock.unlock()
        } else {
            broadcastCurrentFetchState(FETCH_NOTIF_STATE_FINISHED, FETCH_NOTIF_REASON_ALREADY_RUNNING)
        }
        return Result.success()
    }
}
