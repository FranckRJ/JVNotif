package com.franckrj.jvnotif.utils

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.annotation.StringRes
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.util.SimpleArrayMap
import android.widget.Toast
import com.franckrj.jvnotif.MainActivity
import com.franckrj.jvnotif.R

class FetchNotifTool(val context: Context) {
    private val listOfCurrentRequests: ArrayList<GetNumberOfMpForAccount> = ArrayList()
    private val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, String> = SimpleArrayMap()
    var fetchNotifIsFinishedListener: FetchNotifIsFinished? = null
    var showToasts: Boolean = false
    var onlyUpdateAndDontShowNotif: Boolean = false

    companion object {
        val wakeLockTimeout: Long = 120_000
        val EXTRA_SHOW_TOAST: String = "EXTRA_SHOW_TOAST"
        val EXTRA_ONLY_UPDATE_AND_DONT_SHOW_NOTIF: String = "EXTRA_ONLY_UPDATE_AND_DONT_SHOW_NOTIF"
        val ACTION_MP_NUMBER_UPDATED: String = "ACTION_MP_NUMBER_UPDATED"
    }

    private val newNumberOfMpReceivedListener = object : GetNumberOfMpForAccount.NewNumberOfMpReceived {
        override fun onReceiveNewNumberOfMp(nicknameOfAccount: String, numberOfMp: String?, getter: GetNumberOfMpForAccount) {
            listOfCurrentRequests.remove(getter)
            listOfNumberOfMpPerAccounts.put(nicknameOfAccount, numberOfMp ?: "")

            /* Une fois que toutes les requêtes sont terminées on affiche une notif. */
            if (listOfCurrentRequests.isEmpty()) {
                updateMpNumberOfAccountsAndShowThingsIfNeeded()
                fetchNotifIsFinishedListener?.onFetchNotifIsFinished()
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_MP_NUMBER_UPDATED))
            }
        }
    }

    private fun updateMpNumberOfAccountsAndShowThingsIfNeeded() {
        AccountsManager.clearNumberOfMpForAllAccounts()
        for (i: Int in 0 until listOfNumberOfMpPerAccounts.size()) {
            val currentNumberOfMp: Int = (listOfNumberOfMpPerAccounts.valueAt(i).toIntOrNull() ?: 0)

            AccountsManager.setNumberOfMp(listOfNumberOfMpPerAccounts.keyAt(i), currentNumberOfMp)
        }

        if (AccountsManager.thereIsNoMp()) {
            /* Aucun mp non lu. */
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, context)

            showShortToast(R.string.noNewMp)
        } else if (AccountsManager.thereIsNewMpSinceLastSavedInfos() ||
                   !PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
            /* Nouveaux mp non lu ou même nombre qu'avant (et supérieur à 0)
             * mais comme la notif a été effacée on l'affiche de nouveau. */
            if (!onlyUpdateAndDontShowNotif && !MainActivity.isTheActiveActivity) {
                val totalNumberOfMp: Int = AccountsManager.getNumberOfMpForAllAccounts()
                val title: String = if (totalNumberOfMp == 1) {
                    context.getString(R.string.newNumberOfMpSingular)
                } else {
                    context.getString(R.string.newNumberOfMpPlural, totalNumberOfMp.toString())
                }
                val text: String = context.getString(R.string.accountsWithNewMp, AccountsManager.getAllNicknamesThatHaveMp())

                NotifsManager.pushNotifAndUpdateInfos(NotifsManager.NotifTypeInfo.Names.MP, title, text, context)
            }
        } else {
            /* Il y a des mp non lu mais ils correspondent à la notification déjà affichée. */
            showShortToast(R.string.noNewMp)
        }

        AccountsManager.saveNumberOfMp()
    }

    private fun showShortToast(@StringRes textId: Int) {
        if (showToasts) {
            Toast.makeText(context, textId, Toast.LENGTH_SHORT).show()
        }
    }

    fun startFetchNotif() {
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
                showShortToast(R.string.connectToZeroAccount)
            }
        } else {
            showShortToast(R.string.errorActionAlreadyRunning)
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
