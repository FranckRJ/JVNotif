package com.franckrj.jvnotif.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.franckrj.jvnotif.R
import com.franckrj.jvnotif.utils.AccountsManager
import com.franckrj.jvnotif.utils.Utils

class AccountMenuDialogFragment : DialogFragment() {
    var accountNickname: String = ""

    companion object {
        val ARG_ACCOUNT_NICKNAME: String = "com.franckrj.jvnotif.accountmenu.account_nickname"

        private val POS_READ_MP: Int = 0
        private val POS_READ_STARS: Int = 1
        private val POS_SEND_MP: Int = 2
        private val POS_DELETE_ACCOUNT: Int = 3
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        accountNickname = (arguments?.getString(ARG_ACCOUNT_NICKNAME, null) ?: getString(R.string.waitingText))

        builder.setTitle(accountNickname)

        @Suppress("ObjectLiteralToLambda")
        builder.setItems(R.array.choicesForAccountMenu, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                when (which) {
                    POS_READ_MP -> {
                        Utils.openPageForThisNickname("http://www.jeuxvideo.com/messages-prives/boite-reception.php", accountNickname, activity!!)
                        AccountsManager.setNumberOfMp(accountNickname, 0)
                        AccountsManager.saveNumberOfMp()
                    }
                    POS_READ_STARS -> {
                        Utils.openPageForThisNickname("http://www.jeuxvideo.com/profil/" + accountNickname.toLowerCase() + "?mode=abonnements", accountNickname, activity!!)
                        AccountsManager.setNumberOfStars(accountNickname, 0)
                        AccountsManager.saveNumberOfStars()
                    }
                    POS_SEND_MP -> {
                        Utils.openPageForThisNickname("http://www.jeuxvideo.com/messages-prives/nouveau.php", accountNickname, activity!!)
                    }
                    POS_DELETE_ACCOUNT -> {
                        val parentActivity: Activity? = activity
                        if (parentActivity is AskForDeleteAccount) {
                            parentActivity.accountThatWantToBeDeleted(accountNickname)
                        }
                    }
                }

                dialog?.dismiss()
            }
        })

        return builder.create()
    }

    interface AskForDeleteAccount {
        fun accountThatWantToBeDeleted(accountNicknameToDelete: String)
    }
}
