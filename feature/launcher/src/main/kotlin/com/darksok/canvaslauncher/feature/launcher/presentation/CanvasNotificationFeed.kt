package com.darksok.canvaslauncher.feature.launcher.presentation

import android.app.Notification
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetNotificationEntry(
    val key: String,
    val text: String,
    val postedAt: Long,
)

object CanvasNotificationFeed {
    private val entriesByKey = LinkedHashMap<String, WidgetNotificationEntry>()
    private val _entries = MutableStateFlow<List<WidgetNotificationEntry>>(emptyList())
    val entries: StateFlow<List<WidgetNotificationEntry>> = _entries.asStateFlow()

    @Synchronized
    fun replaceAll(notifications: Array<StatusBarNotification>?) {
        entriesByKey.clear()
        notifications.orEmpty()
            .asSequence()
            .mapNotNull { notification -> notification.toWidgetEntryOrNull() }
            .sortedByDescending { entry -> entry.postedAt }
            .forEach { entry ->
                entriesByKey[entry.key] = entry
            }
        publish()
    }

    @Synchronized
    fun upsert(notification: StatusBarNotification?) {
        val entry = notification?.toWidgetEntryOrNull() ?: return
        entriesByKey[entry.key] = entry
        publish()
    }

    @Synchronized
    fun remove(notificationKey: String?) {
        if (notificationKey.isNullOrBlank()) return
        if (entriesByKey.remove(notificationKey) != null) {
            publish()
        }
    }

    @Synchronized
    fun clear() {
        if (entriesByKey.isEmpty()) return
        entriesByKey.clear()
        publish()
    }

    private fun publish() {
        _entries.value = entriesByKey.values
            .sortedByDescending { entry -> entry.postedAt }
            .take(WIDGET_NOTIFICATION_CACHE_LIMIT)
    }
}

private fun StatusBarNotification.toWidgetEntryOrNull(): WidgetNotificationEntry? {
    if (isOngoing || notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
    val extras = notification.extras ?: return null
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        ?.toString()
        ?.trim()
        .orEmpty()
    val text = listOf(
        Notification.EXTRA_TEXT,
        Notification.EXTRA_BIG_TEXT,
        Notification.EXTRA_SUB_TEXT,
    )
        .asSequence()
        .mapNotNull { key -> extras.getCharSequence(key)?.toString()?.trim() }
        .firstOrNull { value -> value.isNotBlank() }
        .orEmpty()
    val normalized = sequenceOf(title, text)
        .map { part -> part.replace('\n', ' ').replace('\r', ' ').trim() }
        .filter { part -> part.isNotBlank() }
        .joinToString(separator = " — ")
        .trim()
    if (normalized.isBlank()) return null
    return WidgetNotificationEntry(
        key = key,
        text = normalized,
        postedAt = postTime,
    )
}

private const val WIDGET_NOTIFICATION_CACHE_LIMIT = 32
