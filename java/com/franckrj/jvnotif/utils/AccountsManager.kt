package com.franckrj.jvnotif.utils

object AccountsManager {
    private val accountsList: ArrayList<AccountInfos> = ArrayList()

    fun getCookieForAccount(nicknameToSearch: String): String {
        accountsList
                .firstOrNull { it.nickname == nicknameToSearch }
                ?.let { return it.cookie }

        return ""
    }

    fun addAccount(nickname: String, cookieValue: String) { accountsList.add(AccountInfos(nickname, "coniunctio=" + cookieValue)) }

    fun removeAccount(nicknameToSearch: String) {
        accountsList
                .filter { it.nickname == nicknameToSearch }
                .forEach { accountsList.remove(it) }
    }

    fun getListOfAccounts(): List<AccountInfos> {
        return accountsList
    }

    fun loadListOfAccounts() {
        val listOfAccountsNames: List<String> = PrefsManager.getString(PrefsManager.StringPref.Names.LIST_OF_NICKNAMES)
                .split(",").filter { it.isNotEmpty() }
        val listOfAccountsCookies: List<String> = PrefsManager.getString(PrefsManager.StringPref.Names.LIST_OF_COOKIES)
                .split(",").filter { it.isNotEmpty() }
        val minSizeOfList = minOf(listOfAccountsNames.size, listOfAccountsCookies.size)

        accountsList.clear()
        @Suppress("LoopToCallChain")
        for (i: Int in 0 until minSizeOfList) {
            accountsList.add(AccountInfos(listOfAccountsNames[i], listOfAccountsCookies[i]))
        }
    }

    fun saveListOfAccounts() {
        var listOfAccountsNamesInString: String = ""
        var listOfAccountsCookiesInString: String = ""

        for (i in accountsList.indices) {
            listOfAccountsNamesInString += accountsList[i].nickname + (if (i < (accountsList.size- 1)) "," else "")
            listOfAccountsCookiesInString += accountsList[i].cookie + (if (i < (accountsList.size- 1)) "," else "")
        }

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NICKNAMES, listOfAccountsNamesInString)
        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_COOKIES, listOfAccountsCookiesInString)
        PrefsManager.applyChanges()
    }

    class AccountInfos(val nickname: String, val cookie: String)
}
