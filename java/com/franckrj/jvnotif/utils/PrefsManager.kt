package com.franckrj.jvnotif.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.support.v4.util.SimpleArrayMap
import com.franckrj.jvnotif.R

object PrefsManager {
    private var currentPrefs: SharedPreferences? = null
    private var currentPrefsEdit: SharedPreferences.Editor? = null
    private val listOfStringPrefs: SimpleArrayMap<StringPref.Names, StringPref> = SimpleArrayMap()

    fun initializeSharedPrefs(currentContext: Context) {
        currentPrefs = currentContext.getSharedPreferences(currentContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        @SuppressLint("CommitPrefEdits")
        currentPrefsEdit = currentPrefs?.edit()

        listOfStringPrefs.put(StringPref.Names.LIST_OF_NICKNAMES, StringPref("pref.listOfNicknames", ""))
        listOfStringPrefs.put(StringPref.Names.LIST_OF_COOKIES, StringPref("pref.listOfCookies", ""))
    }

    fun getString(prefName: StringPref.Names): String {
        val prefInfo: StringPref? = listOfStringPrefs.get(prefName)

        @Suppress("LiftReturnOrAssignment")
        if (prefInfo != null) {
            return currentPrefs?.getString(prefInfo.stringName, prefInfo.defaultValue) ?: ""
        } else {
            return ""
        }
    }

    fun putString(prefName: StringPref.Names, newVal: String) {
        val prefInfo: StringPref? = listOfStringPrefs.get(prefName)

        if (prefInfo != null) {
            currentPrefsEdit?.putString(prefInfo.stringName, newVal)
        }
    }

    fun applyChanges() = currentPrefsEdit?.apply()

    class StringPref(val stringName: String, val defaultValue: String/*,
                     val isInt: Boolean = false, val minVal: Int = 0, val maxVal: Int = 0*/) {
        enum class Names {
            LIST_OF_NICKNAMES, LIST_OF_COOKIES
        }
    }
}
