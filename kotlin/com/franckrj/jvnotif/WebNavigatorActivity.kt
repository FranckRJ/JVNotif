package com.franckrj.jvnotif

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.franckrj.jvnotif.base.AbsToolbarActivity
import com.franckrj.jvnotif.utils.Undeprecator
import com.franckrj.jvnotif.utils.Utils

class WebNavigatorActivity : AbsToolbarActivity() {
    private var navigatorWebView: WebView? = null
    private var currentUrl: String = ""
    private var currentTitle: String = ""

    companion object {
        val EXTRA_URL_LOAD: String = "com.franckrj.jvnotif.webnavigatoractivity.EXTRA_URL_LOAD"
        val EXTRA_COOKIE_TO_USE: String = "com.franckrj.jvnotif.webnavigatoractivity.EXTRA_COOKIE_TO_USE"

        private val SAVE_TITLE_FOR_NAVIGATOR: String = "saveTitleForNavigator"
        private val SAVE_URL_FOR_NAVIGATOR: String = "saveUrlForNavigator"
    }

    private fun updateTitleAndSubtitle() {
        val myActionBar: ActionBar? = supportActionBar
        if (myActionBar != null) {
            myActionBar.title = currentTitle
            myActionBar.subtitle = currentUrl
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webnavigator)
        initToolbar(R.id.toolbar_webnavigator)

        val tmpWebView: WebView = findViewById(R.id.webview_webnavigator)
        navigatorWebView = tmpWebView

        tmpWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                currentUrl = url
                updateTitleAndSubtitle()
            }
        }
        tmpWebView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                currentTitle = title
                updateTitleAndSubtitle()
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        tmpWebView.settings.javaScriptEnabled = true
        tmpWebView.settings.useWideViewPort = true
        tmpWebView.settings.setSupportZoom(true)
        tmpWebView.settings.builtInZoomControls = true
        tmpWebView.settings.displayZoomControls = false
        Undeprecator.webSettingsSetSaveFormData(tmpWebView.settings, false)
        Undeprecator.webSettingsSetSavePassword(tmpWebView.settings, false)

        currentTitle = getString(R.string.app_name)

        if (intent != null && savedInstanceState == null) {
            val newUrlToLoad: String? = intent.getStringExtra(EXTRA_URL_LOAD)
            val newCookiesToUse: String? = intent.getStringExtra(EXTRA_COOKIE_TO_USE)

            /* "... != null" pour activer le smartcast. */
            if (newUrlToLoad != null && !newUrlToLoad.isNullOrEmpty()) {
                currentUrl = newUrlToLoad
                tmpWebView.loadUrl(currentUrl)
            }

            if (newCookiesToUse != null && !newCookiesToUse.isNullOrEmpty()) {
                Undeprecator.cookieManagerRemoveAllCookies(CookieManager.getInstance())
                CookieManager.getInstance().setCookie("http://www.jeuxvideo.com/", newCookiesToUse)
            }
            Utils.suppressNotifForCookieUsageInWebview()
        } else if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString(SAVE_TITLE_FOR_NAVIGATOR, getString(R.string.app_name))
            currentUrl = savedInstanceState.getString(SAVE_URL_FOR_NAVIGATOR, "")

            if (currentUrl.isNotEmpty()) {
                tmpWebView.loadUrl(currentUrl)
            }
        }

        updateTitleAndSubtitle()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_webnavigator, menu)
        return true
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_TITLE_FOR_NAVIGATOR, currentTitle)
        outState.putString(SAVE_URL_FOR_NAVIGATOR, currentUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_open_in_external_browser_webnavigator -> {
                Utils.openLinkInExternalNavigator(currentUrl, this)
                return true
            }
            R.id.action_copy_url_webnavigator -> {
                Utils.putStringInClipboard(currentUrl, this)
                Toast.makeText(this, R.string.copyDone, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_reload_page_webnavigator -> {
                navigatorWebView?.reload()
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (navigatorWebView?.canGoBack() == true) {
            navigatorWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
