package com.franckrj.jvnotif.utils

import java.util.regex.Pattern

object JVCParser {
    private val numberOfMpJVCPattern = Pattern.compile("""<div class=".*?account-mp.*?">[^<]*<span[^c]*class="account-number-mp[^"]*".*?data-val="([^"]*)"""", Pattern.DOTALL)

    fun getNumberOfMpFromPage(pageSource: String): String {
        val numberOfMpJVCMatcher = numberOfMpJVCPattern.matcher(pageSource)

        @Suppress("LiftReturnOrAssignment")
        if (numberOfMpJVCMatcher.find()) {
            return numberOfMpJVCMatcher.group(1)
        } else {
            return ""
        }
    }
}
