package com.franckrj.jvnotif

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.franckrj.jvnotif.base.AbsHomeIsBackActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.WorkerShedulesManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.Undeprecator
import com.franckrj.jvnotif.utils.Utils

class AddAnAccountActivity : AbsHomeIsBackActivity() {
    private var jvcWebView: WebView? = null
    private var nicknameText: EditText? = null

    @Suppress("ObjectLiteralToLambda")
    private val saveCookies: View.OnClickListener = object : View.OnClickListener {
        override fun onClick(view: View?) {
            if (nicknameText != null && nicknameText?.text.toString().isNotEmpty()) {
                /* Récupération du cookie "coniunctio", de manière un peu compliquée mais qui fonctionne. */
                val allCookiesInstring: String = CookieManager.getInstance().getCookie("http://www.jeuxvideo.com/")
                val allCookiesInStringArray: Array<String> = TextUtils.split(allCookiesInstring, ";")

                val connectCookieValue: String? = allCookiesInStringArray
                        .map { TextUtils.split(it.trim(), "=") }
                        .firstOrNull { it.size > 1 && it[0] == "coniunctio" }
                        ?.let { it[1] }

                /* "connectCookieValue != null" pour activer le smartcast. */
                if (connectCookieValue != null && !connectCookieValue.isNullOrEmpty()) {
                    AccountsManager.addAccount(nicknameText?.text.toString().trim(), connectCookieValue)
                    AccountsManager.saveListOfAccounts()

                    Toast.makeText(this@AddAnAccountActivity, R.string.connectionSuccessful, Toast.LENGTH_SHORT).show()

                    WorkerShedulesManager.launchNow(applicationContext)

                    finish()
                    return
                }
            } else {
                Toast.makeText(this@AddAnAccountActivity, R.string.errorNicknameMissingAddAnAccount, Toast.LENGTH_LONG).show()

                return
            }

            Toast.makeText(this@AddAnAccountActivity, R.string.errorCookiesMissingAddAnAccount, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addanaccount)
        initToolbar(R.id.toolbar_addanaccount)

        val tmpWebView: WebView = findViewById(R.id.webview_addanaccount)
        val endActionButton: Button = findViewById(R.id.endaction_button_addanaccount)
        jvcWebView = tmpWebView
        nicknameText = findViewById(R.id.nickname_text_addanaccount)

        endActionButton.setOnClickListener(saveCookies)

        /* Suppression de tout ce qui s'apparente de près ou de loin à un cache, des cookies etc etc
         * pour que la nouvelle connexion puisse se faire sans problèmes. */
        Undeprecator.cookieManagerRemoveAllCookies(CookieManager.getInstance())
        Utils.suppressNotifForCookieUsageInWebview()
        tmpWebView.webViewClient = WebViewClient()
        tmpWebView.webChromeClient = WebChromeClient()

        @SuppressLint("SetJavaScriptEnabled")
        tmpWebView.settings.javaScriptEnabled = true
        Undeprecator.webSettingsSetSaveFormData(tmpWebView.settings, false)
        Undeprecator.webSettingsSetSavePassword(tmpWebView.settings, false)
        tmpWebView.clearCache(true)
        tmpWebView.clearHistory()

        tmpWebView.loadUrl("https://www.jeuxvideo.com/login")

        PrefsManager.putInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED,
                            PrefsManager.getInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED) + 1)
        PrefsManager.applyChanges()
    }

    override fun onResume() {
        super.onResume()
        jvcWebView?.resumeTimers()
        jvcWebView?.onResume()
    }

    override fun onPause() {
        jvcWebView?.onPause()
        jvcWebView?.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        jvcWebView?.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        Toast.makeText(this, getString(R.string.warningNotConnected), Toast.LENGTH_LONG).show()
        super.onBackPressed()
    }
}
