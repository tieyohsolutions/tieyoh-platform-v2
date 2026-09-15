package com.tieyoh.mobile

import android.content.Context
import com.google.gson.Gson

// Persists the last signed-in user locally so "Remember me" can skip the
// password prompt on the next app launch. There's no session token from the
// backend yet — this just remembers who was logged in.
object SessionStore {

    private const val PREFS_NAME = "tieyoh_session"
    private const val KEY_USER = "auth_response"

    private val gson = Gson()

    fun save(context: Context, user: AuthResponse) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun load(context: Context): AuthResponse? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, null) ?: return null

        return try {
            gson.fromJson(json, AuthResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER)
            .apply()
    }
}
