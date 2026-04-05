package com.darksok.canvaslauncher.feature.launcher.presentation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class CanvasNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        CanvasNotificationFeed.replaceAll(runCatching { activeNotifications }.getOrNull())
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        CanvasNotificationFeed.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        CanvasNotificationFeed.upsert(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        CanvasNotificationFeed.remove(sbn?.key)
    }
}
