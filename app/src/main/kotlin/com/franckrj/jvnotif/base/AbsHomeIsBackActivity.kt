package com.franckrj.jvnotif.base

import android.view.MenuItem

abstract class AbsHomeIsBackActivity: AbsToolbarActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        @Suppress("LiftReturnOrAssignment")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }
}
