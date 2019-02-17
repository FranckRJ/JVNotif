package com.franckrj.jvnotif.utils

import androidx.collection.SimpleArrayMap

object AccountsManager {
    private val accountsList: ArrayList<AccountInfos> = ArrayList()

    /* ------------ PARTIE GESTION DES MP / STARS */

    fun clearNumberOfMpAndStarsForAllAccounts() = accountsList.forEach { it.numberOfMp = 0; it.numberOfStars = 0 }

    fun getNumberOfMpForAllAccounts(): Int = accountsList.sumBy { it.numberOfMp.coerceAtLeast(0) }

    fun getNumberOfStarsForAllAccounts(): Int = accountsList.sumBy { it.numberOfStars.coerceAtLeast(0) }

    fun getAllNicknamesThatHaveMp(): String = accountsList.filter { it.numberOfMp > 0 }.joinToString(separator = ", ", transform = { it.nickname })

    fun getAllNicknamesThatHaveStars(): String = accountsList.filter { it.numberOfStars > 0 }.joinToString(separator = ", ", transform = { it.nickname })

    fun setNumberOfMpAndStars(forThisNickname: String, newNumbers: MpAndStarsNumbers) {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == forThisNickname.toLowerCase() }
                ?.let { it.numberOfMp = newNumbers.mpNumber; it.numberOfStars = newNumbers.starsNumber }
    }

    fun setNumberOfMp(forThisNickname: String, newNumber: Int) {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == forThisNickname.toLowerCase() }
                ?.let { it.numberOfMp = newNumber }
    }

    fun setNumberOfStars(forThisNickname: String, newNumber: Int) {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == forThisNickname.toLowerCase() }
                ?.let { it.numberOfStars = newNumber }
    }

    fun thereIsNewMpSinceLastSavedInfos(): Boolean {
        return thereIsNewMpOrStarsSinceLastSavedInfos(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_MP, { it.numberOfMp })
    }

    fun thereIsNewStarsSinceLastSavedInfos(): Boolean {
        return thereIsNewMpOrStarsSinceLastSavedInfos(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_STARS, { it.numberOfStars })
    }

    private fun thereIsNewMpOrStarsSinceLastSavedInfos(prefNameOfSavedInfos: PrefsManager.StringPref.Names, getMpOrStarsFromAccount: (AccountInfos) -> Int): Boolean {
        val listOfNumberOfMpOrStarsPerAccounts: SimpleArrayMap<String, Int> = getNumberOfMpOrStarsInfosFromSavedInfos(prefNameOfSavedInfos)

        for (account: AccountInfos in accountsList) {
            val lastNumberOfMpOrStarsForAccount: Int = (listOfNumberOfMpOrStarsPerAccounts.get(account.nickname.toLowerCase()) ?: 0)

            if (getMpOrStarsFromAccount(account) > 0 && getMpOrStarsFromAccount(account) != lastNumberOfMpOrStarsForAccount) {
                return true
            }
        }

        return false
    }

    fun thereIsNoMp(): Boolean = accountsList.none { it.numberOfMp > 0 }

    fun thereIsNoStars(): Boolean = accountsList.none { it.numberOfStars > 0 }

    private fun getNumberOfMpOrStarsInfosFromSavedInfos(prefNameOfSavedInfos: PrefsManager.StringPref.Names): SimpleArrayMap<String, Int> {
        val listOfNumberOfMpOrStarsPerAccounts: SimpleArrayMap<String, Int> = SimpleArrayMap()
        val listOfNumberOfMpOrStarsInfos: List<String> = PrefsManager.getString(prefNameOfSavedInfos)
                .split(",")

        @Suppress("LoopToCallChain")
        for (numberOfMpOrStarsInfos: String in listOfNumberOfMpOrStarsInfos) {
            val listOfInfos: List<String> = numberOfMpOrStarsInfos.split("=")

            if (listOfInfos.size == 2) {
                listOfNumberOfMpOrStarsPerAccounts.put(listOfInfos[0].toLowerCase(), (listOfInfos[1].toIntOrNull() ?: 0))
            }
        }

        return listOfNumberOfMpOrStarsPerAccounts
    }

    private fun loadNumberOfMp() {
        val listOfNumberOfMpPerAccounts: SimpleArrayMap<String, Int> = getNumberOfMpOrStarsInfosFromSavedInfos(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_MP)

        for (i: Int in 0 until listOfNumberOfMpPerAccounts.size()) {
            accountsList
                    .firstOrNull { it.nickname.toLowerCase() == listOfNumberOfMpPerAccounts.keyAt(i).toLowerCase() }
                    ?.let { it.numberOfMp = listOfNumberOfMpPerAccounts.valueAt(i) }
        }
    }

    private fun loadNumberOfStars() {
        val listOfNumberOfStarsPerAccounts: SimpleArrayMap<String, Int> = getNumberOfMpOrStarsInfosFromSavedInfos(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_STARS)

        for (i: Int in 0 until listOfNumberOfStarsPerAccounts.size()) {
            accountsList
                    .firstOrNull { it.nickname.toLowerCase() == listOfNumberOfStarsPerAccounts.keyAt(i).toLowerCase() }
                    ?.let { it.numberOfStars = listOfNumberOfStarsPerAccounts.valueAt(i) }
        }
    }

    fun saveNumberOfMp() {
        val numberOfMpPerAccountInString: String =
                accountsList.joinToString(separator = ",", transform = { it.nickname.toLowerCase() + "=" + it.numberOfMp.toString() })

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_MP, numberOfMpPerAccountInString)
        PrefsManager.applyChanges()
    }

    fun saveNumberOfStars() {
        val numberOfStarsPerAccountInString: String =
                accountsList.joinToString(separator = ",", transform = { it.nickname.toLowerCase() + "=" + it.numberOfStars.toString() })

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NUMBER_OF_STARS, numberOfStarsPerAccountInString)
        PrefsManager.applyChanges()
    }

    /* ------------ PARTIE GESTION DES COMPTES */

    fun getCookieForAccount(nicknameToSearch: String): String {
        accountsList
                .firstOrNull { it.nickname.toLowerCase() == nicknameToSearch.toLowerCase() }
                ?.let { return it.cookie }

        return ""
    }

    fun addAccount(nickname: String, cookieValue: String) {
        /* Suppression du compte s'il est déjà présent pour utiliser le nouveau cookie à la place (et le mettre en fin de liste). */
        accountsList.removeAll { it.nickname.toLowerCase() == nickname.toLowerCase() }

        @Suppress("ConvertToStringTemplate")
        accountsList.add(AccountInfos(nickname, "coniunctio=" + cookieValue))
    }

    fun removeAccount(nicknameToSearch: String) {
        accountsList
                .filter { it.nickname.toLowerCase() == nicknameToSearch.toLowerCase() }
                .forEach { accountsList.remove(it) }
    }

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
            if (listOfAccountsNames[i].isNotEmpty()) {
                accountsList.add(AccountInfos(listOfAccountsNames[i], listOfAccountsCookies[i]))
            }
        }

        loadNumberOfMp()
        loadNumberOfStars()
    }

    fun saveListOfAccounts() {
        val listOfAccountsNamesInString: String = accountsList.joinToString(separator = ",", transform = { it.nickname })
        val listOfAccountsCookiesInString: String = accountsList.joinToString(separator = ",", transform = { it.cookie })

        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_NICKNAMES, listOfAccountsNamesInString)
        PrefsManager.putString(PrefsManager.StringPref.Names.LIST_OF_COOKIES, listOfAccountsCookiesInString)
        PrefsManager.applyChanges()
    }

    class MpAndStarsNumbers (val mpNumber: Int, val starsNumber: Int) {
        companion object {
            const val PARSING_ERROR: Int = -1
            const val NETWORK_ERROR: Int = -2
        }

        fun thereIsANetworkError(): Boolean = (mpNumber == NETWORK_ERROR && starsNumber == NETWORK_ERROR)
    }

    class AccountInfos(val nickname: String, val cookie: String, var numberOfMp: Int = 0, var numberOfStars: Int = 0)
}
