package com.franckrj.jvnotif.utils

import java.util.regex.Pattern

object JVCParser {
    private val numberOfMPJVCPattern = Pattern.compile("""<div class=".*?account-mp.*?">[^<]*<span[^c]*class="account-number-mp[^"]*".*?data-val="([^"]*)"""", Pattern.DOTALL)

    fun getNumberOfMPFromPage(pageSource: String): String {
        val numberOfMpJVCMatcher = numberOfMPJVCPattern.matcher(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfMpJVCMatcher.find()) {
            return numberOfMpJVCMatcher.group(1)
        } else {
            return ""
        }
    }
}
