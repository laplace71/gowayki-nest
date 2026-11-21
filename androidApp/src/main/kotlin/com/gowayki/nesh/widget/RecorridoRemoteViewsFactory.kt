package com.gowayki.nesh.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.gowayki.nesh.R

class RecorridoRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<RecorridoStopItem> = emptyList()

    override fun onCreate() {
        items = RecorridoWidgetData.loadStops(context)
    }

    override fun onDataSetChanged() {
        items = RecorridoWidgetData.loadStops(context)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_recorrido_item)

        if (item.isHeader) {
            val views = RemoteViews(context.packageName, R.layout.widget_recorrido_header)
            views.setTextViewText(R.id.widget_header_title, item.name)
            views.setTextViewText(R.id.widget_header_subtitle, item.district ?: "")
            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widget_header_title, fillInIntent)
            return views
        }

        val views = RemoteViews(context.packageName, R.layout.widget_recorrido_item)
        val stopNumber = stopIndexFor(position)
        val isLastStop = isLastStopInSection(position)
        val label = when {
            stopNumber == 1 -> "A"
            isLastStop -> "B"
            else -> stopNumber.toString()
        }
        views.setTextViewText(R.id.widget_item_index, label)
        views.setTextViewText(R.id.widget_item_name, item.name)
        if (item.district.isNullOrBlank()) {
            views.setViewVisibility(R.id.widget_item_district, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_item_district, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_district, item.district)
        }

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_name, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_index, fillInIntent)
        return views
    }

    private fun stopIndexFor(position: Int): Int {
        var count = 0
        for (i in 0..position) {
            if (!items[i].isHeader) count++
            if (items[i].isHeader && i < position) count = 0
        }
        return count
    }

    private fun isLastStopInSection(position: Int): Boolean {
        for (i in position + 1 until items.size) {
            if (items[i].isHeader) return true
            return false
        }
        return true
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
