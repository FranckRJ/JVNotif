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
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.Undeprecator
import com.franckrj.jvnotif.utils.Utils

class WebBrowserActivity : AbsToolbarActivity() {
    private var browserWebView: WebView? = null
    private var currentUrl: String = ""
    private var currentTitle: String = ""

    companion object {
        const val EXTRA_URL_LOAD: String = "com.franckrj.jvnotif.webbrowseractivity.EXTRA_URL_LOAD"
        const val EXTRA_COOKIE_TO_USE: String = "com.franckrj.jvnotif.webbrowseractivity.EXTRA_COOKIE_TO_USE"

        private const val SAVE_TITLE_FOR_BROWSER: String = "saveTitleForBrowser"
        private const val SAVE_URL_FOR_BROWSER: String = "saveUrlForBrowser"
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
        setContentView(R.layout.activity_webbrowser)
        initToolbar(R.id.toolbar_webbrowser)

        val tmpWebView: WebView = findViewById(R.id.webview_webbrowser)
        browserWebView = tmpWebView

        /* Les cookies doivent être set avant le set web/chrome client et avant le chargement de l'url.
         * La première contrainte est totalement arbitraire, la seconde est logique. */
        if (intent != null && savedInstanceState == null) {
            val newCookiesToUse: String? = intent.getStringExtra(EXTRA_COOKIE_TO_USE)

            Undeprecator.cookieManagerRemoveAllCookies(CookieManager.getInstance())
            if (newCookiesToUse != null && !newCookiesToUse.isNullOrEmpty()) {
                CookieManager.getInstance().setCookie("http://www.jeuxvideo.com/", newCookiesToUse)
            }
            Utils.suppressNotifForCookieUsageInWebview()
        }

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

            /* "... != null" pour activer le smartcast. */
            if (newUrlToLoad != null && !newUrlToLoad.isNullOrEmpty()) {
                currentUrl = newUrlToLoad
                tmpWebView.loadUrl(currentUrl)
            }
        } else if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString(SAVE_TITLE_FOR_BROWSER, getString(R.string.app_name))
            currentUrl = savedInstanceState.getString(SAVE_URL_FOR_BROWSER, "")

            if (currentUrl.isNotEmpty()) {
                tmpWebView.loadUrl(currentUrl)
            }
        }

        updateTitleAndSubtitle()

        PrefsManager.putInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED,
                            PrefsManager.getInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED) + 1)
        PrefsManager.applyChanges()
    }

    override fun onResume() {
        super.onResume()
        browserWebView?.resumeTimers()
        browserWebView?.onResume()
    }

    override fun onPause() {
        browserWebView?.onPause()
        browserWebView?.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        browserWebView?.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_webbrowser, menu)
        return true
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_TITLE_FOR_BROWSER, currentTitle)
        outState.putString(SAVE_URL_FOR_BROWSER, currentUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_open_in_external_browser_webbrowser -> {
                Utils.openLinkInExternalBrowser(currentUrl, this)
                return true
            }
            R.id.action_copy_url_webbrowser -> {
                Utils.putStringInClipboard(currentUrl, this)
                Toast.makeText(this, R.string.copyDone, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_reload_page_webbrowser -> {
                browserWebView?.reload()
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (browserWebView?.canGoBack() == true) {
            browserWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
