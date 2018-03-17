package com.franckrj.jvnotif.base

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.franckrj.jvnotif.utils.Undeprecator
import android.content.Intent
import android.support.v7.app.AlertDialog
import com.franckrj.jvnotif.AddAnAccountActivity
import com.franckrj.jvnotif.NavigationMenuAdapter
import com.franckrj.jvnotif.NavigationMenuListView
import com.franckrj.jvnotif.R
import com.franckrj.jvnotif.dialogs.AccountMenuDialogFragment
import com.franckrj.jvnotif.dialogs.AutocheckPeriodTimePickerDialogFragment
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.InitShedulesManager
import com.franckrj.jvnotif.utils.PrefsManager

abstract class AbsNavigationViewActivity: AbsToolbarActivity(), AccountMenuDialogFragment.AskForDeleteAccount, AutocheckPeriodTimePickerDialogFragment.NewCheckPeriodTimePicked {
    protected val listOfMenuItem: ArrayList<NavigationMenuAdapter.MenuItemInfo> = ArrayList()
    protected var layoutForDrawer: DrawerLayout? = null
    protected var navigationMenuList: NavigationMenuListView? = null
    protected var adapterForNavigationMenu: NavigationMenuAdapter? = null
    protected var toggleForDrawer: ActionBarDrawerToggle? = null
    protected var lastItemSelected: Int = -1
    protected var lastAccountNicknameSelected: String = ""
    /* Id de l'activité de base à highlight par défaut dans le drawer. */
    protected var idOfBaseActivity: Int = -1
    protected var updateMenuOnNextOnResume: Boolean = false
    protected var lastAccountNicknameAskedToBeDeleted: String = ""

    companion object {
        const val GROUP_ID_BASIC: Int = 0
        const val GROUP_ID_ACCOUNT: Int = 1
        const val ITEM_ID_HOME: Int = 0
        const val ITEM_ID_AUTOCHECK_PERIOD_TIME: Int = 1
        const val ITEM_ID_ADD_ACCOUNT: Int = 2
        const val ITEM_ID_SELECT_ACCOUNT: Int = 3
    }

    @Suppress("ObjectLiteralToLambda")
    protected val itemInNavigationClickedListener = object : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if ((adapterForNavigationMenu?.getGroupIdOfRow(id.toInt()) ?: -1) == GROUP_ID_ACCOUNT) {
                lastItemSelected = ITEM_ID_SELECT_ACCOUNT
                lastAccountNicknameSelected = (adapterForNavigationMenu?.getTextOfRow(id.toInt()) ?: "")
            } else {
                lastItemSelected = (adapterForNavigationMenu?.getItemIdOfRow(id.toInt()) ?: -1)
            }

            layoutForDrawer?.closeDrawer(GravityCompat.START)
            adapterForNavigationMenu?.rowSelected = id.toInt()
            adapterForNavigationMenu?.notifyDataSetChanged()
        }
    }

    @Suppress("ObjectLiteralToLambda")
    protected val onClickInDeleteConfirmationPopupListener = object : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            if (which == DialogInterface.BUTTON_POSITIVE)  {
                AccountsManager.removeAccount(lastAccountNicknameAskedToBeDeleted)
                AccountsManager.saveListOfAccounts()
                updateNavigationMenu()
            }
        }
    }

    protected fun initListOfItem() {
        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.home),
                                                              false,
                                                              R.drawable.ic_home_dark_zoom,
                                                              false,
                                                              true,
                                                              ITEM_ID_HOME,
                                                              GROUP_ID_BASIC))

        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.checkForNewMpAndStarsWithInfos, getString(R.string.waitingText)),
                                                              true,
                                                              R.drawable.ic_timer_dark_zoom,
                                                              false,
                                                              true,
                                                              ITEM_ID_AUTOCHECK_PERIOD_TIME,
                                                              GROUP_ID_BASIC))

        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.accounts),
                                                              false,
                                                              0,
                                                              true,
                                                              true,
                                                              -1,
                                                              -1))

        listOfMenuItem.add(NavigationMenuAdapter.MenuItemInfo(getString(R.string.addAnAccount),
                                                              false,
                                                              R.drawable.ic_content_add_dark_zoom,
                                                              false,
                                                              true,
                                                              ITEM_ID_ADD_ACCOUNT,
                                                              GROUP_ID_BASIC))
    }

    protected fun resetSelectRow() {
        adapterForNavigationMenu?.rowSelected = (adapterForNavigationMenu?.getPositionDependingOfId(idOfBaseActivity, GROUP_ID_BASIC) ?: -1)
        adapterForNavigationMenu?.notifyDataSetChanged()
    }

    protected fun updateNavigationMenu() {
        val positionOfAutocheckPeriodTime: Int = (adapterForNavigationMenu?.getPositionDependingOfId(ITEM_ID_AUTOCHECK_PERIOD_TIME, GROUP_ID_BASIC) ?: 0)
        val indexOfAutocheckPeriodTimeInfo: Int = PrefsManager.allAutocheckPeriodTimes.indexOf(PrefsManager.getLong(PrefsManager.LongPref.Names.AUTOCHECK_PERIOD_TIME))
        val arrayOfAutocheckPeriodTimeInfo: Array<String>? = resources?.getStringArray(R.array.choicesForAutocheckPeriodTime)
        val autocheckPeriodTimeInfo: String = if (arrayOfAutocheckPeriodTimeInfo != null && indexOfAutocheckPeriodTimeInfo >= 0 && indexOfAutocheckPeriodTimeInfo < arrayOfAutocheckPeriodTimeInfo.size) {
                                                  arrayOfAutocheckPeriodTimeInfo[indexOfAutocheckPeriodTimeInfo]
                                              } else {
                                                  getString(R.string.waitingText)
                                              }

        adapterForNavigationMenu?.setRowText(positionOfAutocheckPeriodTime, getString(R.string.checkForNewMpAndStarsWithInfos, autocheckPeriodTimeInfo))

        adapterForNavigationMenu?.removeAllItemsFromGroup(GROUP_ID_ACCOUNT)

        val listOfAccountNickames: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()
        var positionOfAddAcountItem: Int = (adapterForNavigationMenu?.getPositionDependingOfId(ITEM_ID_ADD_ACCOUNT, GROUP_ID_BASIC) ?: 0)

        for (account in listOfAccountNickames) {
            listOfMenuItem.add(positionOfAddAcountItem, NavigationMenuAdapter.MenuItemInfo(account.nickname,
                                                                                           false,
                                                                                           0,
                                                                                           false,
                                                                                           true,
                                                                                           -1,
                                                                                           GROUP_ID_ACCOUNT))
            ++positionOfAddAcountItem
        }

        resetSelectRow()
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
                    ITEM_ID_AUTOCHECK_PERIOD_TIME -> {
                        val argForFrag = Bundle()
                        val periodTimePickerDialogFragment = AutocheckPeriodTimePickerDialogFragment()
                        argForFrag.putLong(AutocheckPeriodTimePickerDialogFragment.ARG_CURRENT_REFRESH_TIME, PrefsManager.getLong(PrefsManager.LongPref.Names.AUTOCHECK_PERIOD_TIME))
                        argForFrag.putLongArray(AutocheckPeriodTimePickerDialogFragment.ARG_ALL_REFRESH_TIMES, PrefsManager.allAutocheckPeriodTimes)
                        periodTimePickerDialogFragment.arguments = argForFrag
                        periodTimePickerDialogFragment.show(supportFragmentManager, "AutocheckPeriodTimePickerDialogFragment")
                    }
                    ITEM_ID_ADD_ACCOUNT -> {
                        startActivity(Intent(this@AbsNavigationViewActivity, AddAnAccountActivity::class.java))
                        updateMenuOnNextOnResume = true
                    }
                    ITEM_ID_SELECT_ACCOUNT -> {
                        val argForFrag = Bundle()
                        val accountMenuDialogFragment = AccountMenuDialogFragment()
                        argForFrag.putString(AccountMenuDialogFragment.ARG_ACCOUNT_NICKNAME, lastAccountNicknameSelected)
                        accountMenuDialogFragment.arguments = argForFrag
                        accountMenuDialogFragment.show(supportFragmentManager, "AccountMenuDialogFragment")
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

    override fun accountThatWantToBeDeleted(accountNicknameToDelete: String) {
        val deleteAlertBuilder = AlertDialog.Builder(this)
        lastAccountNicknameAskedToBeDeleted = accountNicknameToDelete
        deleteAlertBuilder.setTitle(R.string.deleteAccount).setMessage(getString(R.string.areYouSureToDeleteThisAccount, lastAccountNicknameAskedToBeDeleted))
                .setPositiveButton(R.string.yes, onClickInDeleteConfirmationPopupListener).setNegativeButton(R.string.no, null).show()
    }

    override fun getNewCheckPeriodTime(newCheckPeriodTime: Long) {
        PrefsManager.putLong(PrefsManager.LongPref.Names.AUTOCHECK_PERIOD_TIME, newCheckPeriodTime)
        PrefsManager.applyChanges()

        if (AccountsManager.getListOfAccounts().isNotEmpty()) {
            InitShedulesManager.initSchedulers(this)
        }

        updateNavigationMenu()
    }

    protected abstract fun initializeViewAndToolbar()
}
