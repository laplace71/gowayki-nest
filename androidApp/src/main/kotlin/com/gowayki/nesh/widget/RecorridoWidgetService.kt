package com.gowayki.nesh.widget

import android.content.Intent
import android.widget.RemoteViewsService

class RecorridoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RecorridoRemoteViewsFactory(applicationContext)
    }
}
