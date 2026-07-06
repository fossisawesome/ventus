package com.fossisawesome.ventus.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.fossisawesome.ventus.work.WidgetUpdater

class GlanceWidgetUpdater(private val context: Context) : WidgetUpdater {
    override suspend fun notifyActiveLocationChanged() {
        VentusWidget().updateAll(context)
    }
}
