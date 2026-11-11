package com.rescate.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rescate.app.data.model.User




class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rescate_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var user: User?
        get() = prefs.getString(KEY_USER, null)?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_USER, value?.let { gson.toJson(it) }).apply()

    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    val isLoggedIn: Boolean get() = token != null && user != null

    fun isChannelEnabled(channelId: String): Boolean = prefs.getBoolean("notif_$channelId", true)

    fun setChannelEnabled(channelId: String, enabled: Boolean) =
        prefs.edit().putBoolean("notif_$channelId", enabled).apply()

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt"
        private const val KEY_USER = "user"
        private const val KEY_ONBOARDING = "onboarding_seen"

        @Volatile
        private var instance: SessionManager? = null

        fun get(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
    }
}
