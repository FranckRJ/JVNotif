package com.franckrj.jvnotif.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

object JVCParser {
    private val numberOfMpJVCPattern = Pattern.compile("""<div class=".*?account-mp.*?">[^<]*<span[^c]*class="jv-account-number-mp[^"]*".*?data-val="([^"]*)"""", Pattern.DOTALL)
    private val numberOfStarsJVCPattern = Pattern.compile("<div class=\".*?account-notif.*?\">[^<]*<span[^c]*class=\"jv-account-number-notif[^\"]*\".*?data-val=\"([^\"]*)\"", Pattern.DOTALL)

    fun getNumberOfMpFromPage(pageSource: String): Int {
        val numberOfMpJVCMatcher: Matcher = numberOfMpJVCPattern.matcher(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfMpJVCMatcher.find()) {
            return numberOfMpJVCMatcher.group(1).toIntOrNull() ?: -1
        } else {
            return -1
        }
    }

    fun getNumberOfStarsFromPage(pageSource: String): Int {
        val numberOfStarsJVCMatcher: Matcher = numberOfStarsJVCPattern.matcher(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfStarsJVCMatcher.find()) {
            return numberOfStarsJVCMatcher.group(1).toIntOrNull() ?: -1
        } else {
            return -1
        }
    }
}
