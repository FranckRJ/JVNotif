package com.franckrj.jvnotif

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.util.SimpleArrayMap
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.franckrj.jvnotif.base.AbsNavigationViewActivity
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.JVCParser
import com.franckrj.jvnotif.utils.NotifsManager
import com.franckrj.jvnotif.utils.WebManager

/*TODO: Gérer les recréations d'activités pour la récup' des notifs.*/
class MainActivity : AbsNavigationViewActivity() {
    private val listOfCurrentRequests: ArrayList<GetNumberOfMPForAccount> = ArrayList()
    private val listOfNumberOfMPPerAccounts: SimpleArrayMap<String, String> = SimpleArrayMap()

    init {
        idOfBaseActivity = ITEM_ID_HOME
    }

    @Suppress("ObjectLiteralToLambda")
    private val checkNotifClickedListener: View.OnClickListener = object : View.OnClickListener {
        override fun onClick(view: View?) {
            if (listOfCurrentRequests.isEmpty()) {
                val listOfAccounts: List<AccountsManager.AccountInfos> = AccountsManager.getListOfAccounts()

                listOfNumberOfMPPerAccounts.clear()

                if (listOfAccounts.isNotEmpty()) {
                    for (account in listOfAccounts) {
                        val newRequest = GetNumberOfMPForAccount(account.nickname, account.cookie)

                        listOfCurrentRequests.add(newRequest)
                        newRequest.execute()
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.connectToZeroAccount, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, R.string.errorActionAlreadyRunning, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun newNumberOfMPReceived(forNickname: String, newNumberOfMP: String?, withThisRequest: GetNumberOfMPForAccount) {
        listOfCurrentRequests.remove(withThisRequest)
        listOfNumberOfMPPerAccounts.put(forNickname, newNumberOfMP ?: "")

        /*Une fois que toutes les requêtes sont terminées on affiche une notif.*/
        if (listOfCurrentRequests.isEmpty()) {
            makeAndPushNotificationForMP()
        }
    }

    private fun makeAndPushNotificationForMP() {
        var totalNumberOfMP: Int = 0
        var text: String = ""

        for (i in 0 until listOfNumberOfMPPerAccounts.size()) {
            val currentNumberOfMP: Int = (listOfNumberOfMPPerAccounts.valueAt(i).toIntOrNull() ?: 0)

            totalNumberOfMP += currentNumberOfMP
            if (currentNumberOfMP > 0) {
                text += listOfNumberOfMPPerAccounts.keyAt(i) + ", "
            }
        }

        text = text.removeSuffix(", ")
        text = getString(R.string.accountsWithNewMP, text)

        if (totalNumberOfMP > 0) {
            val title: String = getString(R.string.newNumberOfMP, totalNumberOfMP.toString())

            NotifsManager.pushNotif(NotifsManager.NotifTypeInfo.Names.MP, title, text, this)
        } else {
            Toast.makeText(this, R.string.noNewMP, Toast.LENGTH_SHORT).show()
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
    }

    override fun onPause() {
        val iterator: MutableListIterator<GetNumberOfMPForAccount> = listOfCurrentRequests.listIterator()
        while (iterator.hasNext()) {
            iterator.next().cancel(true)
            iterator.remove()
        }
        super.onPause()
    }

    /*TODO: Changer la manière dont l'AsyncTask est géré (plus de inner).*/
    private inner class GetNumberOfMPForAccount(val nickname: String, val cookie: String) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg p0: Void?): String? {
            val currentWebInfos = WebManager.WebInfos()
            val pageContent: String?

            currentWebInfos.followRedirects = false
            /*TODO: Check pour changer le lien de la requête (si besoin).*/
            pageContent = WebManager.sendRequest("http://www.jeuxvideo.com/sso/settings.php", "GET", "", cookie, currentWebInfos)

            @Suppress("LiftReturnOrAssignment")
            if (pageContent != null) {
                return JVCParser.getNumberOfMPFromPage(pageContent)
            } else {
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            newNumberOfMPReceived(nickname, result, this)
        }
    }
}
