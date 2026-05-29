package com.example.kaishelvesapp.data.local

import android.content.Context
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.ActivityNotificationItem
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object FriendsLocalStore {
    private const val PREFS_NAME = "kai_friends_cache"
    private const val HOME_FEED_PREFIX = "home_feed_"
    private const val ACTIVITY_NOTIFICATIONS_PREFIX = "activity_notifications_"
    private const val RECEIVED_REQUESTS_PREFIX = "received_requests_"
    private const val MAX_HOME_ACTIVITIES = 60
    private const val MAX_NOTIFICATIONS = 100
    private const val MAX_RECEIVED_REQUESTS = 50

    private val gson: Gson = GsonBuilder().create()
    private val homeFeedType = object : TypeToken<List<FriendActivityItem>>() {}.type
    private val activityNotificationsType = object : TypeToken<List<ActivityNotificationItem>>() {}.type
    private val receivedRequestsType = object : TypeToken<List<Usuario>>() {}.type

    private fun prefs() = AppContextProvider.requireContext().getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun readHomeFeed(uid: String): List<FriendActivityItem> {
        return readList<FriendActivityItem>(uid, HOME_FEED_PREFIX, homeFeedType)
            .filter { activity -> activity.id.isNotBlank() }
    }

    @Synchronized
    fun writeHomeFeed(uid: String, activities: List<FriendActivityItem>) {
        writeList(
            uid = uid,
            prefix = HOME_FEED_PREFIX,
            items = activities
                .filter { activity -> activity.id.isNotBlank() }
                .distinctBy { activity -> activity.id }
                .take(MAX_HOME_ACTIVITIES)
        )
    }

    @Synchronized
    fun readActivityNotifications(uid: String): List<ActivityNotificationItem> {
        return readList<ActivityNotificationItem>(uid, ACTIVITY_NOTIFICATIONS_PREFIX, activityNotificationsType)
            .filter { notification -> notification.id.isNotBlank() }
    }

    @Synchronized
    fun writeActivityNotifications(uid: String, notifications: List<ActivityNotificationItem>) {
        writeList(
            uid = uid,
            prefix = ACTIVITY_NOTIFICATIONS_PREFIX,
            items = notifications
                .filter { notification -> notification.id.isNotBlank() }
                .distinctBy { notification -> notification.id }
                .take(MAX_NOTIFICATIONS)
        )
    }

    @Synchronized
    fun readReceivedRequests(uid: String): List<Usuario> {
        return readList<Usuario>(uid, RECEIVED_REQUESTS_PREFIX, receivedRequestsType)
            .filter { user -> user.uid.isNotBlank() }
    }

    @Synchronized
    fun writeReceivedRequests(uid: String, requests: List<Usuario>) {
        writeList(
            uid = uid,
            prefix = RECEIVED_REQUESTS_PREFIX,
            items = requests
                .filter { user -> user.uid.isNotBlank() }
                .distinctBy { user -> user.uid }
                .take(MAX_RECEIVED_REQUESTS)
        )
    }

    private fun <T> readList(uid: String, prefix: String, type: java.lang.reflect.Type): List<T> {
        if (uid.isBlank()) return emptyList()
        val raw = prefs().getString(prefix + uid, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<T>>(raw, type) }
            .getOrNull()
            .orEmpty()
    }

    private fun <T> writeList(uid: String, prefix: String, items: List<T>) {
        if (uid.isBlank()) return
        prefs()
            .edit()
            .putString(prefix + uid, gson.toJson(items))
            .apply()
    }
}
