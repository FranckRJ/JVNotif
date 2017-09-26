package com.franckrj.jvnotif

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.franckrj.jvnotif.base.AbsNavigationViewActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.FetchNotifTool
import com.franckrj.jvnotif.utils.InitShedulesManager

class MainActivity : AbsNavigationViewActivity() {
    init {
        idOfBaseActivity = ITEM_ID_HOME
    }

    @Suppress("ObjectLiteralToLambda")
    private val checkNotifClickedListener = object : View.OnClickListener {
        override fun onClick(view: View?) {
            val fetchNotifIntent: Intent = Intent(this@MainActivity, FetchNotifService::class.java)
            fetchNotifIntent.putExtra(FetchNotifTool.EXTRA_SHOW_TOAST, true)
            startService(fetchNotifIntent)
        }
    }

    override fun initializeViewAndToolbar() {
        setContentView(R.layout.activity_main)
        initToolbar(R.id.toolbar_main)

        layoutForDrawer = findViewById(R.id.layout_drawer_main)
        navigationMenuList = findViewById(R.id.navigation_menu_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkNotifButton: Button = findViewById(R.id.checknotif_button_main)

        checkNotifButton.setOnClickListener(checkNotifClickedListener)

        if (AccountsManager.getListOfAccounts().isNotEmpty()) {
            InitShedulesManager.initSchedulerAndReceiver(this)
        }
    }
}
