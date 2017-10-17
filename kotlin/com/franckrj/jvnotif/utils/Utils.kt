package com.franckrj.jvnotif.utils

import android.content.ClipData
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

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
}
