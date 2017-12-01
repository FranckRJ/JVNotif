package com.franckrj.jvnotif.utils

import android.content.ClipData
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import com.franckrj.jvnotif.WebNavigatorActivity

object Utils {
    fun openLinkInExternalNavigator(link: String, parentActivity: Activity) {
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
        clipboardService.primaryClip = clip
    }

    fun openPageForThisNickname(pageLink: String, nicknameToUse: String, fromThisActivity: Activity) {
        val newNavigatorIntent = Intent(fromThisActivity, WebNavigatorActivity::class.java)
        newNavigatorIntent.putExtra(WebNavigatorActivity.EXTRA_URL_LOAD, pageLink)
        newNavigatorIntent.putExtra(WebNavigatorActivity.EXTRA_COOKIE_TO_USE, AccountsManager.getCookieForAccount(nicknameToUse))

        fromThisActivity.startActivity(newNavigatorIntent)
    }

    fun suppressNotifForCookieUsageInWebview() {
        CookieManager.getInstance().setCookie("http://www.jeuxvideo.com/", "wbCookieNotifier=1")
    }
}
