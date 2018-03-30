package com.franckrj.jvnotif.utils

object JVCParser {
    private val numberOfMpJVCPattern = Regex("""<div class=".*?account-mp.*?">[^<]*<span[^c]*class="jv-account-number-mp[^"]*".*?data-val="([^"]*)"""", RegexOption.DOT_MATCHES_ALL)
    private val numberOfStarsJVCPattern = Regex("""<div class=".*?account-notif.*?">[^<]*<span[^c]*class="jv-account-number-notif[^"]*".*?data-val="([^"]*)"""", RegexOption.DOT_MATCHES_ALL)

    fun getNumberOfMpFromPage(pageSource: String): Int {
        val numberOfMpJVCMatcher: MatchResult? = numberOfMpJVCPattern.find(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfMpJVCMatcher != null) {
            return numberOfMpJVCMatcher.groupValues[1].toIntOrNull() ?: -1
        } else {
            return -1
        }
    }

    fun getNumberOfStarsFromPage(pageSource: String): Int {
        val numberOfStarsJVCMatcher: MatchResult? = numberOfStarsJVCPattern.find(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfStarsJVCMatcher != null) {
            return numberOfStarsJVCMatcher.groupValues[1].toIntOrNull() ?: -1
        } else {
            return -1
        }
    }
}
