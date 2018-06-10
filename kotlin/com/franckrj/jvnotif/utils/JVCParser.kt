package com.franckrj.jvnotif.utils

object JVCParser {
    private val numberOfMpJVCPattern = Regex("""<div class=".*?account-mp.*?">[^<]*<span[^c]*class="jv-account-number-mp[^"]*".*?data-val="([^"]*)"""", RegexOption.DOT_MATCHES_ALL)
    private val numberOfStarsJVCPattern = Regex("""<div class=".*?account-notif.*?">[^<]*<span[^c]*class="jv-account-number-notif[^"]*".*?data-val="([^"]*)"""", RegexOption.DOT_MATCHES_ALL)

    fun getMpAndStarsNumbersFromPage(pageSource: String): AccountsManager.MpAndStarsNumbers {
        var mpNumberFromPage: Int = AccountsManager.MpAndStarsNumbers.PARSING_ERROR
        var starsNumberFromPage: Int = AccountsManager.MpAndStarsNumbers.PARSING_ERROR

        val numberOfMpJVCMatcher: MatchResult? = numberOfMpJVCPattern.find(pageSource)
        val numberOfStarsJVCMatcher: MatchResult? = numberOfStarsJVCPattern.find(pageSource)

        if (numberOfMpJVCMatcher != null) {
            mpNumberFromPage = numberOfMpJVCMatcher.groupValues[1].toIntOrNull() ?: AccountsManager.MpAndStarsNumbers.PARSING_ERROR
        }
        if (numberOfStarsJVCMatcher != null) {
            starsNumberFromPage = numberOfStarsJVCMatcher.groupValues[1].toIntOrNull() ?: AccountsManager.MpAndStarsNumbers.PARSING_ERROR
        }

        return AccountsManager.MpAndStarsNumbers(mpNumberFromPage, starsNumberFromPage)
    }
}
