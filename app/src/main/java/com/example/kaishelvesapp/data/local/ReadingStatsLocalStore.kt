package com.example.kaishelvesapp.data.local

import android.content.Context
import com.example.kaishelvesapp.data.model.ReadingStatsSnapshot
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object ReadingStatsLocalStore {
    private const val PREFS_NAME = "kai_reading_stats_cache"
    private const val KEY_PREFIX = "stats_"

    private val gson: Gson = GsonBuilder().create()

    private fun prefs() = AppContextProvider.requireContext().getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun read(ownerId: String): ReadingStatsSnapshot? {
        val rawStats = prefs().getString(KEY_PREFIX + ownerId, null).orEmpty()
        return runCatching {
            gson.fromJson(rawStats, ReadingStatsSnapshot::class.java)
        }.getOrNull()
    }

    @Synchronized
    fun write(ownerId: String, snapshot: ReadingStatsSnapshot) {
        prefs()
            .edit()
            .putString(KEY_PREFIX + ownerId, gson.toJson(snapshot))
            .apply()
    }
}
