package com.franckrj.jvnotif

import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView
import android.widget.ListView

class NavigationMenuListView : ListView {
    private var listViewPadding: Int = 0

    @TargetApi(21)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        initializeNavigationMenuList(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        initializeNavigationMenuList(context)
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
        initializeNavigationMenuList(context)
    }

    constructor(context: Context)
            : super(context) {
        initializeNavigationMenuList(context)
    }

    fun setHeaderView(headerView: View) {
        /* "spacingView" représente l'espace entre le header et le premier élément de la liste, aucun
         * autre moyen plus simple n'a été trouvé. */
        val spacingView = View(headerView.context)
        spacingView.layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, listViewPadding)
        addHeaderView(headerView, null, false)
        addHeaderView(spacingView, null, false)
    }

    private fun initializeNavigationMenuList(context: Context) {
        listViewPadding = context.resources.getDimensionPixelSize(R.dimen.paddingOfNavigationMenuListView)

        overScrollMode = View.OVER_SCROLL_NEVER
        clipToPadding = false
        setDrawSelectorOnTop(true)
        divider = null
        dividerHeight = 0
        isVerticalScrollBarEnabled = true
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        setPadding(0, 0, 0, listViewPadding)
    }
}
