package com.franckrj.jvnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.franckrj.jvnotif.base.AbsNavigationViewActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.FetchNotifTool
import com.franckrj.jvnotif.utils.InitShedulesManager
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.Utils

class MainActivity : AbsNavigationViewActivity() {
    private var notifInfoText: TextView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var adapterForNotifList: NotifListAdapter? = null

    companion object {
        val EXTRA_MP_NOTIF_CANCELED: String = "com.franckrj.jvnotif.mainactivity.EXTRA_MP_NOTIF_CANCELED"

        private val SAVE_NOTIF_INFO_TEXT: String = "SAVE_NOTIF_INFO_TEXT"

        var isTheActiveActivity: Boolean = false
    }

    init {
        idOfBaseActivity = ITEM_ID_HOME
    }

    @Suppress("ObjectLiteralToLambda")
    private val swipeRefreshActivatedListener = object : SwipeRefreshLayout.OnRefreshListener {
        override fun onRefresh() {
            startService(Intent(this@MainActivity, FetchNotifService::class.java))
        }
    }

    private val mpNotifClickedListener = object : NotifListAdapter.NotifViewHolder.ItemClickedListener {
        override fun onItemClickedListener(nicknameOfItem: String) {
            Utils.openPageForThisNickname("http://www.jeuxvideo.com/messages-prives/boite-reception.php", nicknameOfItem, this@MainActivity)
            AccountsManager.setNumberOfMp(nicknameOfItem, 0)
            AccountsManager.saveNumberOfMp()
        }
    }

    private val fetchNotifStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newFetchNotifState: Int = intent.getIntExtra(FetchNotifTool.EXTRA_NEW_FETCH_NOTIF_STATE, FetchNotifTool.FETCH_NOTIF_STATE_INVALID)
            val reasonForState: Int = intent.getIntExtra(FetchNotifTool.EXTRA_FETCH_NOTIF_STATE_REASON, FetchNotifTool.FETCH_NOTIF_REASON_NO_REASON)

            when (newFetchNotifState) {
                FetchNotifTool.FETCH_NOTIF_STATE_STARTED -> {
                    swipeRefresh?.isRefreshing = true
                    notifInfoText?.visibility = View.GONE
                    adapterForNotifList?.listOfAccounts = ArrayList()
                }
                FetchNotifTool.FETCH_NOTIF_STATE_FINISHED -> {
                    if (reasonForState != FetchNotifTool.FETCH_NOTIF_REASON_ALREADY_RUNNING) {
                        swipeRefresh?.isRefreshing = false
                        setNotifListToCurrentNotifsAndUpdateInfoVisibility()

                        when (reasonForState) {
                            FetchNotifTool.FETCH_NOTIF_REASON_NO_ACCOUNT -> {
                                notifInfoText?.setText(R.string.connectToZeroAccount)
                            }
                            else -> {
                                notifInfoText?.setText(R.string.noNewMp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setNotifListToCurrentNotifsAndUpdateInfoVisibility() {
        adapterForNotifList?.listOfAccounts = AccountsManager.getListOfAccounts().filter { it.numberOfMp > 0 }

        if ((adapterForNotifList?.itemCount ?: 0) > 0) {
            notifInfoText?.visibility = View.GONE
        } else {
            notifInfoText?.visibility = View.VISIBLE
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

        val notifList: RecyclerView = findViewById(R.id.notiflist_main)
        notifInfoText = findViewById(R.id.notifinfo_text_main)
        swipeRefresh = findViewById(R.id.swiperefresh_main)

        adapterForNotifList = NotifListAdapter(this)
        adapterForNotifList?.onItemClickedListener = mpNotifClickedListener
        notifList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        notifList.layoutManager = LinearLayoutManager(this)
        notifList.adapter = adapterForNotifList
        swipeRefresh?.setOnRefreshListener(swipeRefreshActivatedListener)
        swipeRefresh?.setColorSchemeResources(R.color.colorAccent)

        if (savedInstanceState == null) {
            val openedFromNotif: Boolean = consumeIntent(intent)

            startService(Intent(this, FetchNotifService::class.java))

            /* On n'initialise pas les schedulers si on a ouvert l'appli via une notif ou si la notif
             * était visible, parce que dans ce cas ils étaient forcément déjà initialisés. */
            if (AccountsManager.getListOfAccounts().isNotEmpty() && !openedFromNotif &&
                    !PrefsManager.getBool(PrefsManager.BoolPref.Names.MP_NOTIF_IS_VISIBLE)) {
                InitShedulesManager.initSchedulers(this)
            }
        } else {
            val currentNotifInfoText: String? = savedInstanceState.getString(SAVE_NOTIF_INFO_TEXT, null)

            if (currentNotifInfoText != null) {
                notifInfoText?.text = currentNotifInfoText
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
                             .registerReceiver(fetchNotifStateChangedReceiver, IntentFilter(FetchNotifTool.ACTION_FETCH_NOTIF_STATE_CHANGED))

        /* On màj dans le onResume parce que c'est plus simple (même si moins logique) de le faire ici qu'à chaque fois quand :
         *     - on créé l'activité
         *     - on ajoute un compte
         *     - on lit les MP depuis accountWithMpList
         *     - on lit les MP via le menu latéral
         */
        setNotifListToCurrentNotifsAndUpdateInfoVisibility()

        /* On supprime la notification car la liste affiche déjà la même chose. */
        NotifsManager.cancelNotifAndClearInfos(NotifsManager.NotifTypeInfo.Names.MP, this)
    }

    override fun onPause() {
        isTheActiveActivity = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchNotifStateChangedReceiver)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_NOTIF_INFO_TEXT, notifInfoText?.text?.toString())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    private class NotifListAdapter(val context: Context) : RecyclerView.Adapter<NotifListAdapter.NotifViewHolder>() {
        private val serviceInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var onItemClickedListener: NotifViewHolder.ItemClickedListener? = null
        var listOfAccounts: List<AccountsManager.AccountInfos> = ArrayList()
            set(newList) {
                field = newList
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): NotifViewHolder {
            return NotifViewHolder(serviceInflater.inflate(R.layout.notif_row, parent, false), onItemClickedListener)
        }

        override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
            holder.setInformations(listOfAccounts[position].nickname,
                                   context.getString(R.string.mpNumber, listOfAccounts[position].numberOfMp.toString()))
        }

        override fun getItemCount(): Int = listOfAccounts.size

        class NotifViewHolder(mainView: View, onItemClickedListener: ItemClickedListener?) : RecyclerView.ViewHolder(mainView) {
            private val nicknameView: TextView = mainView.findViewById(R.id.nickname_notif_row)
            private val notifView: TextView = mainView.findViewById(R.id.notif_notif_row)

            init {
                val clickableView: View = mainView.findViewById(R.id.clickable_layout_notif_row)
                clickableView.setOnClickListener({
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
