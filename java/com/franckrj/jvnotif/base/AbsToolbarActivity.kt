package com.franckrj.jvnotif.base

import android.support.annotation.IdRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

abstract class AbsToolbarActivity: AppCompatActivity() {
    protected fun initToolbar(@IdRes idOfToolbar: Int) {
        val myToolbar: Toolbar = findViewById(idOfToolbar)
        setSupportActionBar(myToolbar)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
