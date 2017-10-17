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
    private val listOfBoolPrefs: SimpleArrayMap<BoolPref.Names, BoolPref> = SimpleArrayMap()

    fun initializeSharedPrefs(currentContext: Context) {
        currentPrefs = currentContext.getSharedPreferences(currentContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        @SuppressLint("CommitPrefEdits")
        currentPrefsEdit = currentPrefs?.edit()

        listOfStringPrefs.put(StringPref.Names.LIST_OF_NICKNAMES, StringPref("pref.listOfNicknames", ""))
        listOfStringPrefs.put(StringPref.Names.LIST_OF_COOKIES, StringPref("pref.listOfCookies", ""))
        listOfStringPrefs.put(StringPref.Names.LIST_OF_NUMBER_OF_MP, StringPref("pref.listOfNumberOfMp", ""))
        listOfBoolPrefs.put(BoolPref.Names.MP_NOTIF_IS_VISIBLE, BoolPref("pref.mpNotifIsVisible", false))
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

    fun getBool(prefName: BoolPref.Names): Boolean {
        val prefInfo: BoolPref? = listOfBoolPrefs.get(prefName)

        @Suppress("LiftReturnOrAssignment")
        if (prefInfo != null) {
            return currentPrefs?.getBoolean(prefInfo.stringName, prefInfo.defaultValue) ?: false
        } else {
            return false
        }
    }

    fun putBool(prefName: BoolPref.Names, newVal: Boolean) {
        val prefInfo: BoolPref? = listOfBoolPrefs.get(prefName)

        if (prefInfo != null) {
            currentPrefsEdit?.putBoolean(prefInfo.stringName, newVal)
        }
    }

    fun applyChanges() = currentPrefsEdit?.apply()

    class StringPref(val stringName: String, val defaultValue: String) {
        enum class Names {
            LIST_OF_NICKNAMES, LIST_OF_COOKIES, LIST_OF_NUMBER_OF_MP
        }
    }

    class BoolPref(val stringName: String, val defaultValue: Boolean) {
        enum class Names {
            MP_NOTIF_IS_VISIBLE
        }
    }
}
