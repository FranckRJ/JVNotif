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
    private val listOfIntPrefs: SimpleArrayMap<IntPref.Names, IntPref> = SimpleArrayMap()

    fun initializeSharedPrefs(currentContext: Context) {
        currentPrefs = currentContext.getSharedPreferences(currentContext.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        @SuppressLint("CommitPrefEdits")
        currentPrefsEdit = currentPrefs?.edit()

        listOfStringPrefs.put(StringPref.Names.LIST_OF_NICKNAMES, StringPref("pref.listOfNicknames", ""))
        listOfStringPrefs.put(StringPref.Names.LIST_OF_COOKIES, StringPref("pref.listOfCookies", ""))
        listOfBoolPrefs.put(BoolPref.Names.MP_NOTIF_IS_VISIBLE, BoolPref("pref.mpNotifIsVisible", false))
        listOfIntPrefs.put(IntPref.Names.LAST_NUMBER_OF_MP_FETCHED, IntPref("pref.lastNumberOfMPFetched", -1))
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

    fun getInt(prefName: IntPref.Names): Int {
        val prefInfo: IntPref? = listOfIntPrefs.get(prefName)

        @Suppress("LiftReturnOrAssignment")
        if (prefInfo != null) {
            return currentPrefs?.getInt(prefInfo.stringName, prefInfo.defaultValue) ?: 0
        } else {
            return 0
        }
    }

    fun putInt(prefName: IntPref.Names, newVal: Int) {
        val prefInfo: IntPref? = listOfIntPrefs.get(prefName)

        if (prefInfo != null) {
            currentPrefsEdit?.putInt(prefInfo.stringName, newVal)
        }
    }

    fun applyChanges() = currentPrefsEdit?.apply()

    class StringPref(val stringName: String, val defaultValue: String/*,
                     val isInt: Boolean = false, val minVal: Int = 0, val maxVal: Int = 0*/) {
        enum class Names {
            LIST_OF_NICKNAMES, LIST_OF_COOKIES
        }
    }

    class BoolPref(val stringName: String, val defaultValue: Boolean) {
        enum class Names {
            MP_NOTIF_IS_VISIBLE
        }
    }

    class IntPref(val stringName: String, val defaultValue: Int) {
        enum class Names {
            LAST_NUMBER_OF_MP_FETCHED
        }
    }
}
