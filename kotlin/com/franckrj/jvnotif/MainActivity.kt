package com.franckrj.jvnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.franckrj.jvnotif.base.AbsNavigationViewActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.FetchNotifTool
import com.franckrj.jvnotif.utils.InitShedulesManager
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.Utils

class MainActivity : AbsNavigationViewActivity() {
    private var checkNotifButton: Button? = null
    private var adapterForAccountWithMp: AccountListAdapter? = null

    companion object {
        val EXTRA_MP_NOTIF_CANCELED: String = "com.franckrj.jvnotif.mainactivity.EXTRA_MP_NOTIF_CANCELED"

        var isTheActiveActivity: Boolean = false
    }

    init {
        idOfBaseActivity = ITEM_ID_HOME
    }

    @Suppress("ObjectLiteralToLambda")
    private val checkNotifClickedListener = object : View.OnClickListener {
        override fun onClick(view: View?) {
            val fetchNotifIntent = Intent(this@MainActivity, FetchNotifService::class.java)
            fetchNotifIntent.putExtra(FetchNotifTool.EXTRA_SHOW_TOAST, true)
            fetchNotifIntent.putExtra(FetchNotifTool.EXTRA_ONLY_UPDATE_AND_DONT_SHOW_NOTIF, true)
            startService(fetchNotifIntent)
        }
    }

    private val accountClickedListener = object : AccountListAdapter.AccountViewHolder.ItemClickedListener {
        override fun onItemClickedListener(nicknameOfItem: String) {
            Utils.openPageForThisNickname("http://www.jeuxvideo.com/messages-prives/boite-reception.php", nicknameOfItem, this@MainActivity)
            AccountsManager.setNumberOfMp(nicknameOfItem, 0)
            AccountsManager.saveNumberOfMp()
        }
    }

    private val numberOfMpUpdatedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, this@MainActivity)
            updateAccountWithMpList()
        }
    }

    private fun updateAccountWithMpList() {
        adapterForAccountWithMp?.listOfAccounts = AccountsManager.getListOfAccounts().filter { it.numberOfMp > 0 }

        if ((adapterForAccountWithMp?.itemCount ?: 0) > 0) {
            checkNotifButton?.visibility = View.GONE
        } else {
            checkNotifButton?.visibility = View.VISIBLE
        }
    }

    fun consumeIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(EXTRA_MP_NOTIF_CANCELED, false) == true) {
            NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, this)
            return true
        }
        return false
    }

    override fun initializeViewAndToolbar() {
        setContentView(R.layout.activity_main)
        initToolbar(R.id.toolbar_main)

        layoutForDrawer = findViewById(R.id.layout_drawer_main)
        navigationMenuList = findViewById(R.id.navigation_menu_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountWithMpList: RecyclerView = findViewById(R.id.accountwithmp_list_main)
        checkNotifButton = findViewById(R.id.checknotif_button_main)

        adapterForAccountWithMp = AccountListAdapter(this)
        adapterForAccountWithMp?.onItemClickedListener = accountClickedListener
        accountWithMpList.layoutManager = LinearLayoutManager(this)
        accountWithMpList.adapter = adapterForAccountWithMp
        checkNotifButton?.setOnClickListener(checkNotifClickedListener)

        if (savedInstanceState == null) {
            val openedFromNotif: Boolean = consumeIntent(intent)
            val fetchNotifIntent = Intent(this, FetchNotifService::class.java)

            fetchNotifIntent.putExtra(FetchNotifTool.EXTRA_ONLY_UPDATE_AND_DONT_SHOW_NOTIF, true)
            startService(fetchNotifIntent)

            /* On n'initialise pas les schedulers si on a ouvert l'appli via une notif ou si la notif
             * était visible, parce que dans ce cas ils étaient forcément déjà initialisés. */
            if (AccountsManager.getListOfAccounts().isNotEmpty() && !openedFromNotif &&
                    !PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
                InitShedulesManager.initSchedulers(this)
            }
        }

        //vidage du cache des webviews
        if (PrefsManager.getInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED) > 15) {
            val tmpWebView = WebView(this)
            tmpWebView.clearCache(true)
            PrefsManager.putInt(PrefsManager.IntPref.Names.NUMBER_OF_WEBVIEW_OPEN_SINCE_CACHE_CLEARED, 0)
            PrefsManager.applyChanges()
        }
    }

    override fun onResume() {
        super.onResume()
        isTheActiveActivity = true

        LocalBroadcastManager.getInstance(this)
                             .registerReceiver(numberOfMpUpdatedReceiver, IntentFilter(FetchNotifTool.ACTION_MP_NUMBER_UPDATED))
        updateAccountWithMpList()

        /* On supprime la notification car la liste affiche déjà la même chose. */
        NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, this)
    }

    override fun onPause() {
        isTheActiveActivity = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(numberOfMpUpdatedReceiver)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private class AccountListAdapter(val context: Context) :
            RecyclerView.Adapter<AccountListAdapter.AccountViewHolder>() {
        private val serviceInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var onItemClickedListener: AccountViewHolder.ItemClickedListener? = null
        var listOfAccounts: List<AccountsManager.AccountInfos> = ArrayList()
            set(newList) {
                field = newList
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AccountViewHolder {
            return AccountViewHolder(serviceInflater.inflate(R.layout.accountwithmp_row, parent, false), onItemClickedListener)
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            holder.setInformations(listOfAccounts[position].nickname,
                                   context.getString(R.string.mpNumber, listOfAccounts[position].numberOfMp.toString()))
        }

        override fun getItemCount(): Int = listOfAccounts.size

        class AccountViewHolder(mainView: View, onItemClickedListener: ItemClickedListener?) : RecyclerView.ViewHolder(mainView) {
            private val nicknameView: TextView = mainView.findViewById(R.id.nickname_accountwithmp_row)
            private val notifView: TextView = mainView.findViewById(R.id.notif_accountwithmp_row)

            init {
                val image: ImageView = mainView.findViewById(R.id.image_accountwithmp_row)
                image.setColorFilter(Color.rgb(253, 83, 46))

                mainView.setOnClickListener({
                    onItemClickedListener?.onItemClickedListener(nicknameView.text.toString())
                })
            }

            fun setInformations(newNickname: String, newNotif: String) {
                nicknameView.text = newNickname
                notifView.text = newNotif
            }

            interface ItemClickedListener {
                fun onItemClickedListener(nicknameOfItem: String)
            }
        }
    }
}
