package com.franckrj.jvnotif.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.text.Html
import android.text.Spanned

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

    fun htmlFromHtml(source: String): Spanned {
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            return Html.fromHtml(source)
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
