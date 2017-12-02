package com.franckrj.jvnotif.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.franckrj.jvnotif.R

class AutocheckPeriodTimePickerDialogFragment : DialogFragment() {
    companion object {
        val ARG_CURRENT_REFRESH_TIME: String = "com.franckrj.jvnotif.autocheckperiodtimepicker.current_refresh_time"

        private val equivalentInMsForChoices: LongArray = longArrayOf(900_000,  1_800_000,  3_600_000,  43_200_000, 86_400_000)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        val currentRefreshTime: Long = (arguments?.getLong(ARG_CURRENT_REFRESH_TIME, 0) ?: 0)
        /* Retourne -1 si l'élément n'est pas trouvé, ce qui correspond à aucun item sélectionné dans le
         * singleChoiceItems, ce qui est le comportement attendu. */
        val currentSelectedItem: Int = equivalentInMsForChoices.indexOf(currentRefreshTime)

        builder.setTitle(R.string.checkForNewMp)

        @Suppress("ObjectLiteralToLambda")
        builder.setSingleChoiceItems(R.array.choicesForAutocheckPeriodTime, currentSelectedItem, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (which >= 0 && which < equivalentInMsForChoices.size) {
                    val parentActivity: Activity? = activity
                    if (parentActivity is NewCheckPeriodTimePicked) {
                        parentActivity.getNewCheckPeriodTime(equivalentInMsForChoices[which])
                    }
                }
                dialog?.dismiss()
            }
        })

        return builder.create()
    }

    interface NewCheckPeriodTimePicked {
        fun getNewCheckPeriodTime(newCheckPeriodTime: Long)
    }
}
