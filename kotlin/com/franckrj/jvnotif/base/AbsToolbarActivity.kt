package com.franckrj.jvnotif.base

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

abstract class AbsToolbarActivity: AppCompatActivity() {
    protected fun initToolbar(@IdRes idOfToolbar: Int) {
        val myToolbar: Toolbar = findViewById(idOfToolbar)
        setSupportActionBar(myToolbar)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
