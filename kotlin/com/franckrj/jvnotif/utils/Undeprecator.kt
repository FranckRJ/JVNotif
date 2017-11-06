package com.franckrj.jvnotif.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.webkit.CookieManager
import android.webkit.WebSettings

object Undeprecator {
    @ColorInt
    fun  resourcesGetColor(resources: Resources, @ColorRes colorId: Int): Int {
        if (Build.VERSION.SDK_INT >= 23) {
            return resources.getColor(colorId, null)
        } else {
            @Suppress("DEPRECATION")
            return resources.getColor(colorId)
        }
    }

    fun resourcesGetDrawable(resources: Resources, @DrawableRes drawableId: Int): Drawable {
        if (Build.VERSION.SDK_INT >= 21) {
            return resources.getDrawable(drawableId, null)
        } else {
            @Suppress("DEPRECATION")
            return resources.getDrawable(drawableId)
        }
    }

    fun cookieManagerRemoveAllCookies(manager: CookieManager) {
        if (Build.VERSION.SDK_INT >= 21) {
            manager.removeAllCookies(null)
        } else {
            @Suppress("DEPRECATION")
            manager.removeAllCookie()
        }
    }

    fun webSettingsSetSaveFormData(settings: WebSettings, newVal: Boolean) {
        @Suppress("DEPRECATION")
        settings.saveFormData = newVal
    }

    fun webSettingsSetSavePassword(settings: WebSettings, newVal: Boolean) {
        @Suppress("DEPRECATION")
        settings.savePassword = newVal
    }
}
