package com.franckrj.jvnotif.utils

import android.support.v4.util.SimpleArrayMap

object AccountsManager {
    private val accountsList: ArrayList<AccountInfos> = ArrayList()

    /* PARTIE GESTION DES MP */

    fun clearNumberOfMpForAllAccounts() = accountsList.forEach { it.numberOfMp = 0 }

    fun getNumberOfMpForAllAccounts(): Int = accountsList.sumBy { it.numberOfMp }

    fun getAllNicknamesThatHaveMp(): String = accountsList.filter { it.numberOfMp > 0 }.joinToString(separator = ", ", transform = { it.nickname })

    fun setNumberOfMp(forThisNickname: String, newNumber: Int) {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == forThisNickname.toLowerCase() }
                ?.let { it.numberOfMp = newNumber }
    }

    fun thereIsNewMpSinceLastSavedInfos(): Boolean {
        val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, Int> = getNumberOfMpInfosFromSavedInfos()

        for (account: AccountInfos in accountsList) {
            val lastNumberOfMpForAccount: Int = (listOfNumberOfMpPerAccounts.get(account.nickname.toLowerCase()) ?: 0)

            if (account.numberOfMp > 0 && account.numberOfMp != lastNumberOfMpForAccount) {
                return true
            }
        }

        return false
    }

    fun thereIsNoMp(): Boolean = accountsList.none { it.numberOfMp > 0 }

    private fun getNumberOfMpInfosFromSavedInfos(): SimpleArrayMap<String, Int> {
        val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, Int> = SimpleArrayMap()
        val listOfNumberOfMpInfos: List<String> = PrefsManager.getString(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_MP)
                .split(",")

        @Suppress("LoopToCallChain")
        for (numberOfMpInfos: String in listOfNumberOfMpInfos) {
            val listOfInfos: List<String> = numberOfMpInfos.split("=")

            if (listOfInfos.size == 2) {
                listOfNumberOfMpPerAccounts.put(listOfInfos[0].toLowerCase(), (listOfInfos[1].toIntOrNull() ?: 0))
            }
        }

        return listOfNumberOfMpPerAccounts
    }

    private fun loadNumberOfMp() {
        val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, Int> = getNumberOfMpInfosFromSavedInfos()

        for (i: Int in 0 until listOfNumberOfMpPerAccounts.size()) {
            accountsList
                    .firstOrNull { it.nickname.toLowerCase() == listOfNumberOfMpPerAccounts.keyAt(i).toLowerCase() }
                    ?.let { it.numberOfMp = listOfNumberOfMpPerAccounts.valueAt(i) }
        }
    }

    fun saveNumberOfMp() {
        val numberOfMpPerAccountInString: String =
                accountsList.joinToString(separator = ",", transform = { it.nickname.toLowerCase() + "=" + it.numberOfMp.toString() })

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_MP, numberOfMpPerAccountInString)
        PrefsManager.applyChanges()
    }

    /* PARTIE GESTION DES COMPTES */

    fun getCookieForAccount(nicknameToSearch: String): String {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == nicknameToSearch.toLowerCase() }
                ?.let { return it.cookie }

        return ""
    }

    fun addAccount(nickname: String, cookieValue: String) {
        if (accountsList.firstOrNull { it.nickname.toLowerCase() == nickname.toLowerCase() } == null) {
            accountsList.add(AccountInfos(nickname, "coniunctio=" + cookieValue))
        }
    }

    /*fun removeAccount(nicknameToSearch: String) {
        accountsList
                .filter { it.nickname.toLowerCase() == nicknameToSearch.toLowerCase() }
                .forEach { accountsList.remove(it) }
    }*/

    fun getListOfAccounts(): List<AccountInfos> {
        return accountsList
    }

    fun loadListOfAccounts() {
        val listOfAccountsNames: List<String> = PrefsManager.getString(PrefsManager.StringPref.Names.LIST_OF_NICKNAMES)
                .split(",")
        val listOfAccountsCookies: ArrayList<String> = ArrayList(PrefsManager.getString(PrefsManager.StringPref.Names.LIST_OF_COOKIES)
                .split(","))

        while (listOfAccountsCookies.size < listOfAccountsNames.size) {
            listOfAccountsCookies.add("")
        }

        accountsList.clear()
        @Suppress("LoopToCallChain")
        for (i: Int in listOfAccountsNames.indices) {
            accountsList.add(AccountInfos(listOfAccountsNames[i], listOfAccountsCookies[i]))
        }

        loadNumberOfMp()
    }

    fun saveListOfAccounts() {
        val listOfAccountsNamesInString: String = accountsList.joinToString(separator = ",", transform = { it.nickname })
        val listOfAccountsCookiesInString: String = accountsList.joinToString(separator = ",", transform = { it.cookie })

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NICKNAMES, listOfAccountsNamesInString)
        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_COOKIES, listOfAccountsCookiesInString)
        PrefsManager.applyChanges()
    }

    class AccountInfos(val nickname: String, val cookie: String, var numberOfMp: Int = 0)
}
