package com.franckrj.jvnotif.utils

import android.content.ClipData
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import com.franckrj.jvnotif.WebBrowserActivity

object Utils {
    fun openLinkInExternalBrowser(link: String, parentActivity: Activity) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            parentActivity.startActivity(browserIntent)
        } catch (e: Exception) {
            //rien
        }
    }

    fun putStringInClipboard(textToCopy: String, fromThisActivity: Activity) {
        val clipboardService: ClipboardManager = fromThisActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(textToCopy, textToCopy)
        clipboardService.setPrimaryClip(clip)
    }

    fun openPageForThisNickname(pageLink: String, nicknameToUse: String, fromThisActivity: Activity) {
        val newBrowserIntent = Intent(fromThisActivity, WebBrowserActivity::class.java)
        newBrowserIntent.putExtra(WebBrowserActivity.EXTRA_URL_LOAD, pageLink)
        newBrowserIntent.putExtra(WebBrowserActivity.EXTRA_COOKIE_TO_USE, AccountsManager.getCookieForAccount(nicknameToUse))

        fromThisActivity.startActivity(newBrowserIntent)
    }

    fun suppressNotifForCookieUsageInWebview() {
        CookieManager.getInstance().setCookie("https://www.jeuxvideo.com", "_cmpQcif3pcsupported=1");
        CookieManager.getInstance().setCookie("https://jeuxvideo.com", "_gcl_au=1.1.1298996599.1593456467");
        CookieManager.getInstance().setCookie("https://www.jeuxvideo.com", "euconsent=BO1ximpO1ximpAKAiCENDQAAAAAweAAA");
        CookieManager.getInstance().setCookie("https://www.jeuxvideo.com", "googlepersonalization=O1ximpO1ximpAA");
        CookieManager.getInstance().setCookie("https://www.jeuxvideo.com", "noniabvendorconsent=O1ximpO1ximpAKAiAA8AAA");
        CookieManager.getInstance().setCookie("https://www.jeuxvideo.com", "visitor_country=FR");
    }
}
