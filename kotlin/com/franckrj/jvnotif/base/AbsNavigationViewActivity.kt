package com.franckrj.jvnotif.base

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.franckrj.jvnotif.utils.Undeprecator
import android.content.Intent
import com.franckrj.jvnotif.AddAnAccountActivity
import com.franckrj.jvnotif.NavigationMenuAdapter
import com.franckrj.jvnotif.NavigationMenuListView
import com.franckrj.jvnotif.R
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.WebNavigatorActivity

abstract class AbsNavigationViewActivity: AbsToolbarActivity() {
    protected val listOfMenuItem: ArrayList<NavigationMenuAdapter.MenuItemInfo> = ArrayList()
    protected var layoutForDrawer: DrawerLayout? = null
    protected var navigationMenuList: NavigationMenuListView? = null
    protected var adapterForNavigationMenu: NavigationMenuAdapter? = null
    protected var toggleForDrawer: ActionBarDrawerToggle? = null
    protected var lastItemSelected: Int = -1
    protected var lastAccountNameSelected: String = ""
    /* Id de l'activité de base à highlight par défaut dans le drawer. */
    protected var idOfBaseActivity: Int = -1
    protected var updateMenuOnNextOnResume: Boolean = false

    companion object {
        val GROUP_ID_BASIC: Int = 0
        val GROUP_ID_ACCOUNT: Int = 1
        val ITEM_ID_HOME: Int = 0
        val ITEM_ID_ADD_ACCOUNT: Int = 1
        val ITEM_ID_SELECT_ACCOUNT: Int = 2
    }

    @Suppress("ObjectLiteralToLambda")
    protected val itemInNavigationClickedListener = object : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if ((adapterForNavigationMenu?.getGroupIdOfRow(id.toInt()) ?: -1) == GROUP_ID_ACCOUNT) {
                lastItemSelected = ITEM_ID_SELECT_ACCOUNT
                lastAccountNameSelected = (adapterForNavigationMenu?.getTextOfRow(id.toInt()) ?: "")
            } else {
                lastItemSelected = (adapterForNavigationMenu?.getItemIdOfRow(id.toInt()) ?: -1)
            }

            layoutForDrawer?.closeDrawer(GravityCompat.START)
            adapterForNavigationMenu?.rowSelected = id.toInt()
            adapterForNavigationMenu?.notifyDataSetChanged()
        }
    }

    private fun initListOfItem() {
        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.home),
                                                              R.drawable.ic_action_home_dark_zoom,
                                                              false,
                                                              true,
                                                              ITEM_ID_HOME,
                                                              GROUP_ID_BASIC))

        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.accounts),
                                                              0,
                                                              true,
                                                              true,
                                                              -1,
                                                              -1))

        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.addAnAccount),
                                                              R.drawable.ic_action_content_add_dark_zoom,
                                                              false,
                                                              true,
                                                              ITEM_ID_ADD_ACCOUNT,
                                                              GROUP_ID_BASIC))
    }

    private fun resetSelectRow() {
        adapterForNavigationMenu?.rowSelected = (adapterForNavigationMenu?.getPositionDependingOfId(idOfBaseActivity, GROUP_ID_BASIC) ?: -1)
        adapterForNavigationMenu?.notifyDataSetChanged()
    }

    private fun updateNavigationMenu() {
        adapterForNavigationMenu?.removeAllItemsFromGroup(GROUP_ID_ACCOUNT)

        val listOfAccountNames: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()
        var positionOfAddAcountItem: Int = (adapterForNavigationMenu?.getPositionDependingOfId(ITEM_ID_ADD_ACCOUNT, GROUP_ID_BASIC) ?: 0)

        for (account in listOfAccountNames) {
            listOfMenuItem.add(positionOfAddAcountItem, NavigationMenuAdapter.MenuItemInfo(account.nickname,
                                                                                           0,
                                                                                           false,
                                                                                           true,
                                                                                           -1,
                                                                                           GROUP_ID_ACCOUNT))
            ++positionOfAddAcountItem
        }

        resetSelectRow()
    }

    protected fun openMpPageForThisNickname(nicknameToUse: String) {
        val newNavigatorIntent = Intent(this@AbsNavigationViewActivity, WebNavigatorActivity::class.java)
        newNavigatorIntent.putExtra(WebNavigatorActivity.EXTRA_URL_LOAD, "http://www.jeuxvideo.com/messages-prives/boite-reception.php")
        newNavigatorIntent.putExtra(WebNavigatorActivity.EXTRA_COOKIE_TO_USE, AccountsManager.getCookieForAccount(nicknameToUse))

        AccountsManager.setNumberOfMp(nicknameToUse, 0)
        AccountsManager.saveNumberOfMp()

        if (AccountsManager.thereIsNoMp()) {
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, this@AbsNavigationViewActivity)
        }

        startActivity(newNavigatorIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initListOfItem()
        initializeViewAndToolbar()

        val newToggleForDrawer: ActionBarDrawerToggle = object : ActionBarDrawerToggle(this, layoutForDrawer, R.string.openDrawerContentDescRes, R.string.closeDrawerContentDescRes) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                lastItemSelected = -1
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)

                when (lastItemSelected) {
                    ITEM_ID_ADD_ACCOUNT -> {
                        startActivity(Intent(this@AbsNavigationViewActivity, AddAnAccountActivity::class.java))
                        updateMenuOnNextOnResume = true
                    }
                    ITEM_ID_SELECT_ACCOUNT -> {
                        openMpPageForThisNickname(lastAccountNameSelected)
                    }
                }

                resetSelectRow()
            }
        }
        newToggleForDrawer.isDrawerSlideAnimationEnabled = false
        toggleForDrawer = newToggleForDrawer

        val navigationHeader: View = layoutInflater.inflate(R.layout.navigation_view_header, navigationMenuList, false)
        adapterForNavigationMenu = NavigationMenuAdapter(this)
        adapterForNavigationMenu?.selectedItemColor = Undeprecator.resourcesGetColor(resources, android.R.color.black)
        adapterForNavigationMenu?.unselectedItemColor = Undeprecator.resourcesGetColor(resources, R.color.navigationIconColor)
        adapterForNavigationMenu?.selectedBackgroundColor = (Undeprecator.resourcesGetColor(resources, R.color.colorAccent) and 0x40FFFFFF)
        adapterForNavigationMenu?.unselectedBackgroundColor = Undeprecator.resourcesGetColor(resources, android.R.color.transparent)
        adapterForNavigationMenu?.normalTextColor = Undeprecator.resourcesGetColor(resources, android.R.color.black)
        adapterForNavigationMenu?.headerTextColor = Undeprecator.resourcesGetColor(resources, R.color.headerTextColor)
        adapterForNavigationMenu?.setListOfMenuItem(listOfMenuItem)
        navigationMenuList?.setHeaderView(navigationHeader)
        navigationMenuList?.adapter = adapterForNavigationMenu
        navigationMenuList?.onItemClickListener = itemInNavigationClickedListener
        layoutForDrawer?.addDrawerListener(newToggleForDrawer)
        layoutForDrawer?.setDrawerShadow(R.drawable.shadow_drawer, GravityCompat.START)
        updateNavigationMenu()

        /* Sous Android 4.0.3-4.0.4 il est impossible de mettre un drawable en tant que background d'une view
         * (du moins ça marche pas de la même manière), donc en solution de remplacement une couleur unie est utilisé. */
        if (Build.VERSION.SDK_INT > 15) {
            Undeprecator.viewSetBackgroundDrawable(navigationHeader, Undeprecator.resourcesGetDrawable(resources, R.drawable.navigation_header_background))
        } else {
            navigationHeader.setBackgroundColor(Undeprecator.resourcesGetColor(resources, R.color.colorPrimary))
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggleForDrawer?.syncState()
    }

    override fun onResume() {
        super.onResume()

        if (updateMenuOnNextOnResume) {
            updateNavigationMenu()
            updateMenuOnNextOnResume = false
        }
    }

    override fun onBackPressed() {
        if (layoutForDrawer?.isDrawerOpen(GravityCompat.START) == true) {
            layoutForDrawer?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        toggleForDrawer?.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        @Suppress("LiftReturnOrAssignment")
        if (toggleForDrawer?.onOptionsItemSelected(item) == true) {
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    protected abstract fun initializeViewAndToolbar()
}
