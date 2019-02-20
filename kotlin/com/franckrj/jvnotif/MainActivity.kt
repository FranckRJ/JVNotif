package com.franckrj.jvnotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import com.franckrj.jvnotif.base.AbsNavigationViewActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.WorkerShedulesManager
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.PrefsManager
import com.franckrj.jvnotif.utils.Undeprecator
import com.franckrj.jvnotif.utils.Utils

class MainActivity : AbsNavigationViewActivity() {
    private var notifInfoText: TextView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var adapterForNotifList: NotifListAdapter? = null

    companion object {
        const val EXTRA_NOTIF_IS_CANCELED: String = "com.franckrj.jvnotif.mainactivity.EXTRA_NOTIF_IS_CANCELED"
        const val EXTRA_NOTIF_CANCELED_ID: String = "com.franckrj.jvnotif.mainactivity.EXTRA_NOTIF_CANCELED_ID"

        private const val SAVE_NOTIF_INFO_TEXT: String = "SAVE_NOTIF_INFO_TEXT"

        var isTheActiveActivity: Boolean = false
    }

    init {
        idOfBaseActivity = ITEM_ID_HOME
    }

    @Suppress("ObjectLiteralToLambda")
    private val swipeRefreshActivatedListener = object : SwipeRefreshLayout.OnRefreshListener {
        override fun onRefresh() {
            WorkerShedulesManager.launchNow()
        }
    }

    private val notifInListClickedListener = object : NotifListAdapter.NotifViewHolder.NotifClickedListener {
        override fun onNotifClickedListener(nicknameOfNotif: String, notifType: NotifListAdapter.NotifInfo.NotifTypeName) {
            if (notifType == NotifListAdapter.NotifInfo.NotifTypeName.MP) {
                Utils.openPageForThisNickname("http://www.jeuxvideo.com/messages-prives/boite-reception.php", nicknameOfNotif, this@MainActivity)
                AccountsManager.setNumberOfMp(nicknameOfNotif, 0)
                AccountsManager.saveNumberOfMp()
            } else if (notifType == NotifListAdapter.NotifInfo.NotifTypeName.STARS) {
                Utils.openPageForThisNickname("http://www.jeuxvideo.com/profil/" + nicknameOfNotif.toLowerCase() + "?mode=abonnements", nicknameOfNotif, this@MainActivity)
                AccountsManager.setNumberOfStars(nicknameOfNotif, 0)
                AccountsManager.saveNumberOfStars()
            }
        }
    }

    private val fetchNotifStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newFetchNotifState: Int = intent.getIntExtra(FetchNotifWorker.EXTRA_NEW_FETCH_NOTIF_STATE, FetchNotifWorker.FETCH_NOTIF_STATE_INVALID)
            val reasonForState: Int = intent.getIntExtra(FetchNotifWorker.EXTRA_FETCH_NOTIF_STATE_REASON, FetchNotifWorker.FETCH_NOTIF_REASON_NO_REASON)

            when (newFetchNotifState) {
                FetchNotifWorker.FETCH_NOTIF_STATE_STARTED -> {
                    swipeRefresh?.isRefreshing = true
                    notifInfoText?.visibility = View.GONE
                    adapterForNotifList?.listOfNotifs = ArrayList()
                }
                FetchNotifWorker.FETCH_NOTIF_STATE_FINISHED -> {
                    if (reasonForState != FetchNotifWorker.FETCH_NOTIF_REASON_ALREADY_RUNNING) {
                        swipeRefresh?.isRefreshing = false
                        setNotifListToCurrentNotifsAndUpdateInfoVisibility()

                        when (reasonForState) {
                            FetchNotifWorker.FETCH_NOTIF_REASON_NO_ACCOUNT -> {
                                notifInfoText?.setText(R.string.connectToZeroAccount)
                            }
                            FetchNotifWorker.FETCH_NOTIF_REASON_NETWORK_ERROR -> {
                                notifInfoText?.setText(R.string.errorDuringFetchOfMpAndStars)
                            }
                            else -> {
                                notifInfoText?.setText(R.string.noNewMpAndStars)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setNotifListToCurrentNotifsAndUpdateInfoVisibility() {
        var numberOfAccountsWithNetworkError: Int = 0
        val newListOfNotifs: ArrayList<NotifListAdapter.NotifInfo> = ArrayList()
        AccountsManager.getListOfAccounts().forEach {
                                                if (it.numberOfMp > 0) {
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.MP, it.numberOfMp))
                                                }
                                                if (it.numberOfStars > 0) {
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.STARS, it.numberOfStars))
                                                }

                                                if (it.numberOfMp == AccountsManager.MpAndStarsNumbers.NETWORK_ERROR && it.numberOfStars == AccountsManager.MpAndStarsNumbers.NETWORK_ERROR) {
                                                    ++numberOfAccountsWithNetworkError
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.ERROR_NETWORK, 0))
                                                } else if (it.numberOfMp == AccountsManager.MpAndStarsNumbers.PARSING_ERROR && it.numberOfStars == AccountsManager.MpAndStarsNumbers.PARSING_ERROR) {
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.ERROR_PARSING_ALL, 0))
                                                } else if (it.numberOfMp == AccountsManager.MpAndStarsNumbers.PARSING_ERROR) {
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.ERROR_PARSING_MP, 0))
                                                } else if (it.numberOfStars == AccountsManager.MpAndStarsNumbers.PARSING_ERROR) {
                                                    newListOfNotifs.add(NotifListAdapter.NotifInfo(it.nickname, NotifListAdapter.NotifInfo.NotifTypeName.ERROR_PARSING_STARS, 0))
                                                }
                                            }

        /* On affiche la liste uniquement si certains comptes n'ont pas d'erreur réseau, car sinon le message d'erreur est affiché en background et non dans la liste. */
        if (numberOfAccountsWithNetworkError < AccountsManager.getListOfAccounts().size) {
            adapterForNotifList?.listOfNotifs = newListOfNotifs
        }

        if ((adapterForNotifList?.itemCount ?: 0) > 0) {
            notifInfoText?.visibility = View.GONE
        } else {
            notifInfoText?.visibility = View.VISIBLE
        }
    }

    private fun consumeIntent(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(EXTRA_NOTIF_IS_CANCELED, false) == true) {
            NotifsManager.cancelNotifAndClearInfos(intent.getIntExtra(EXTRA_NOTIF_CANCELED_ID, NotifsManager.INVALID_NOTIF_ID), this)
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
        adapterForNotifList?.onNotifClickedListener = notifInListClickedListener
        notifList.layoutManager = LinearLayoutManager(this)
        notifList.adapter = adapterForNotifList
        swipeRefresh?.setOnRefreshListener(swipeRefreshActivatedListener)
        swipeRefresh?.setColorSchemeResources(R.color.colorAccent)

        if (savedInstanceState == null) {
            WorkerShedulesManager.launchNow()

            WorkerShedulesManager.initSchedulers()
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
                             .registerReceiver(fetchNotifStateChangedReceiver, IntentFilter(FetchNotifWorker.ACTION_FETCH_NOTIF_STATE_CHANGED))

        /* On màj dans le onResume parce que c'est plus simple (même si moins logique) de le faire ici qu'à chaque fois quand :
         *     - on créé l'activité
         *     - on ajoute un compte
         *     - on lit les MP / Stars depuis notifList
         *     - on lit les MP / Stars via le menu latéral
         */
        setNotifListToCurrentNotifsAndUpdateInfoVisibility()

        /* On supprime les notifications car la liste affiche déjà la même chose. */
        NotifsManager.cancelNotifAndClearInfos(NotifsManager.MP_NOTIF_ID, this)
        NotifsManager.cancelNotifAndClearInfos(NotifsManager.STARS_NOTIF_ID, this)
    }

    override fun onPause() {
        isTheActiveActivity = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fetchNotifStateChangedReceiver)
        swipeRefresh?.isRefreshing = false
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
        var onNotifClickedListener: NotifViewHolder.NotifClickedListener? = null
        var listOfNotifs: List<NotifInfo> = ArrayList()
            set(newList) {
                field = newList
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
            return NotifViewHolder(serviceInflater.inflate(R.layout.notif_row, parent, false), onNotifClickedListener)
        }

        override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
            holder.setInformations(listOfNotifs[position], context)
        }

        override fun getItemCount(): Int = listOfNotifs.size

        class NotifViewHolder(mainView: View, onItemClickedListener: NotifClickedListener?) : RecyclerView.ViewHolder(mainView) {
            private val notifIconView: ImageView = mainView.findViewById(R.id.icon_notif_row)
            private val notifNicknameView: TextView = mainView.findViewById(R.id.nickname_notif_row)
            private val notifContentView: TextView = mainView.findViewById(R.id.content_notif_row)
            private val notifErrorTextView: TextView = mainView.findViewById(R.id.errortext_notif_row)
            private var typeOfNotif: NotifInfo.NotifTypeName = NotifInfo.NotifTypeName.INVALID

            init {
                val clickableView: View = mainView.findViewById(R.id.clickable_layout_notif_row)
                clickableView.setOnClickListener({
                    onItemClickedListener?.onNotifClickedListener(notifNicknameView.text.toString(), typeOfNotif)
                })
            }

            fun setInformations(newNotifInfo: NotifInfo, context: Context) {
                typeOfNotif = newNotifInfo.notifType
                notifNicknameView.text = newNotifInfo.nickname

                when (typeOfNotif) {
                    NotifInfo.NotifTypeName.MP -> {
                        notifErrorTextView.visibility = View.GONE
                        notifIconView.setImageDrawable(Undeprecator.resourcesGetDrawable(context.resources, R.drawable.ic_mp))
                        notifContentView.text = context.getString(R.string.mpNumber, newNotifInfo.notifNumber.toString())
                    }
                    NotifInfo.NotifTypeName.STARS -> {
                        notifErrorTextView.visibility = View.GONE
                        notifIconView.setImageDrawable(Undeprecator.resourcesGetDrawable(context.resources, R.drawable.ic_stars))

                        if (newNotifInfo.notifNumber > 1) {
                            notifContentView.text = context.getString(R.string.starsPluralNumber, newNotifInfo.notifNumber.toString())
                        } else {
                            notifContentView.text = context.getString(R.string.starsSingularNumber, newNotifInfo.notifNumber.toString())
                        }
                    }
                    else -> {
                        notifErrorTextView.visibility = View.VISIBLE
                        notifIconView.setImageDrawable(Undeprecator.resourcesGetDrawable(context.resources, R.drawable.ic_error))
                        notifContentView.text = ""

                        when (typeOfNotif) {
                            NotifInfo.NotifTypeName.ERROR_NETWORK -> {
                                notifErrorTextView.text = context.getText(R.string.errorDetailedNoNetwork)
                            }
                            NotifInfo.NotifTypeName.ERROR_PARSING_ALL -> {
                                notifErrorTextView.text = context.getText(R.string.errorDetailedParsingAll)
                            }
                            NotifInfo.NotifTypeName.ERROR_PARSING_MP -> {
                                notifErrorTextView.text = context.getText(R.string.errorDetailedParsingMp)
                            }
                            NotifInfo.NotifTypeName.ERROR_PARSING_STARS -> {
                                notifErrorTextView.text = context.getText(R.string.errorDetailedParsingStars)
                            }
                            else -> {
                                notifErrorTextView.text = ""
                            }
                        }
                    }
                }
            }

            interface NotifClickedListener {
                fun onNotifClickedListener(nicknameOfNotif: String, notifType: NotifInfo.NotifTypeName)
            }
        }

        class NotifInfo(val nickname: String, val notifType: NotifTypeName, val notifNumber: Int) {
            enum class NotifTypeName {
                INVALID, MP, STARS, ERROR_NETWORK, ERROR_PARSING_ALL, ERROR_PARSING_MP, ERROR_PARSING_STARS
            }
        }
    }
}
