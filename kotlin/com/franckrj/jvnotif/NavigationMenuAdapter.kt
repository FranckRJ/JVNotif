package com.franckrj.jvnotif

import android.app.Activity
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.franckrj.jvnotif.utils.Undeprecator

class NavigationMenuAdapter(private val parentActivity: Activity) : BaseAdapter() {
    private var listOfMenuItem: ArrayList<MenuItemInfo> = ArrayList()
    private val serviceInflater: LayoutInflater = (parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
    var rowSelected: Int = -1
    @ColorInt var selectedItemColor: Int = -1
    @ColorInt var unselectedItemColor: Int = -1
    @ColorInt var selectedBackgroundColor: Int = -1
    @ColorInt var unselectedBackgroundColor: Int = -1
    @ColorInt var normalTextColor: Int = -1
    @ColorInt var headerTextColor: Int = -1

    fun getItemIDOfRow(position: Int): Int {
        if (position < listOfMenuItem.size) {
            return listOfMenuItem[position].itemID
        }
        return -1
    }

    fun getGroupIDOfRow(position: Int): Int {
        if (position < listOfMenuItem.size) {
            return listOfMenuItem[position].groupID
        }
        return -1
    }

    fun getTextOfRow(position: Int): String {
        if (position < listOfMenuItem.size) {
            return listOfMenuItem[position].textContent
        }
        return ""
    }

    fun getPositionDependingOfID(itemID: Int, groupID: Int): Int {
        if (itemID != -1) {
            for (i: Int in listOfMenuItem.indices) {
                val currentItemInfo: MenuItemInfo = listOfMenuItem[i]
                if (currentItemInfo.itemID == itemID && (groupID == -1 || currentItemInfo.groupID == groupID)) {
                    return i
                }
            }
        }

        return -1
    }

    fun removeAllItemsFromGroup(groupID: Int) {
        listOfMenuItem.filter { it.groupID == groupID }.forEach { listOfMenuItem.remove(it) }
    }

    fun setListOfMenuItem(newList: ArrayList<MenuItemInfo>) {
        listOfMenuItem = newList
    }

    fun setRowEnabled(position: Int, newVal: Boolean) {
        listOfMenuItem.getOrNull(position)?.isEnabled = newVal
    }

    fun setRowText(position: Int, newText: String) {
        listOfMenuItem.getOrNull(position)?.textContent = newText
    }

    override fun getCount(): Int = listOfMenuItem.size

    override fun getItem(position: Int): Any = listOfMenuItem[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewToUse: View
        val holder: CustomViewHolder
        val currentMenuItemInfo: MenuItemInfo = listOfMenuItem[position]

        if (convertView == null) {
            viewToUse = serviceInflater.inflate(R.layout.navigationmenu_row, parent, false)
            holder = CustomViewHolder(viewToUse.findViewById(R.id.content_text_navigationmenu),
                    viewToUse.findViewById(R.id.upper_line_navigationmenu))

            holder.contentTextView.compoundDrawablePadding = (parentActivity.resources.getDimensionPixelSize(R.dimen.paddingForCompoundDrawableNavigationMenu))
            viewToUse.tag = holder
        } else {
            viewToUse = convertView
            holder = (viewToUse.tag as CustomViewHolder)
        }

        holder.contentTextView.text = currentMenuItemInfo.textContent
        holder.contentTextView.alpha = (if (currentMenuItemInfo.isEnabled) 1f else 0.33f)

        if (currentMenuItemInfo.isHeader) {
            holder.upperLineView.visibility = (if (position > 0) View.VISIBLE else View.INVISIBLE)
            holder.contentTextView.setTextColor(headerTextColor)
        } else {
            holder.upperLineView.visibility = View.GONE
            holder.contentTextView.setTextColor(normalTextColor)
        }

        if (currentMenuItemInfo.drawableResID != 0) {
            val compoundDrawable: Drawable = Undeprecator.resourcesGetDrawable(parentActivity.resources, currentMenuItemInfo.drawableResID).mutate()

            if (rowSelected == position && currentMenuItemInfo.isEnabled) {
                compoundDrawable.setColorFilter(selectedItemColor, PorterDuff.Mode.SRC_ATOP)
            } else {
                compoundDrawable.setColorFilter(unselectedItemColor, PorterDuff.Mode.SRC_ATOP)
            }

            holder.contentTextView.setCompoundDrawablesWithIntrinsicBounds(compoundDrawable, null, null, null)
        } else {
            holder.contentTextView.setCompoundDrawables(null, null, null, null)
        }

        if (rowSelected == position && currentMenuItemInfo.isEnabled) {
            viewToUse.setBackgroundColor(selectedBackgroundColor)
        } else {
            viewToUse.setBackgroundColor(unselectedBackgroundColor)
        }

        return viewToUse
    }

    override fun isEnabled(position: Int): Boolean {
        val currentItemInfo: MenuItemInfo = listOfMenuItem[position]
        return !currentItemInfo.isHeader && currentItemInfo.isEnabled
    }

    private class CustomViewHolder(val contentTextView: TextView,
                                   val upperLineView: View)

    class MenuItemInfo(var textContent: String = "",
                       @DrawableRes val drawableResID: Int = 0,
                       val isHeader: Boolean = false,
                       var isEnabled: Boolean = true,
                       val itemID: Int = -1,
                       val groupID: Int = -1)
}
