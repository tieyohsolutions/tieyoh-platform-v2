package com.tieyoh.mobile

import android.content.Context
import com.google.gson.Gson

// Stores the signed-in user's session behind a biometric-gated Android
// Keystore key (see BiometricAuthHelper). Unlike SessionStore (a plain
// local cache used when the user skips biometric setup), the JSON here is
// encrypted and can't be read back without a successful Face/Touch/
// fingerprint unlock — decrypting it always requires going through
// BiometricAuthHelper.decryptWithBiometrics().
object BiometricSessionStore {

    private const val PREFS_NAME = "tieyoh_biometric_session"
    private const val KEY_CIPHERTEXT = "ciphertext"
    private const val KEY_IV = "iv"

    private val gson = Gson()

    fun isEnrolled(context: Context): Boolean {
        return prefs(context).contains(KEY_CIPHERTEXT)
    }

    fun save(context: Context, ciphertextB64: String, ivB64: String) {
        prefs(context).edit()
            .putString(KEY_CIPHERTEXT, ciphertextB64)
            .putString(KEY_IV, ivB64)
            .apply()
    }

    fun readCiphertext(context: Context): Pair<String, String>? {
        val ciphertext = prefs(context).getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = prefs(context).getString(KEY_IV, null) ?: return null
        return ciphertext to iv
    }

    fun parseUser(json: String): AuthResponse? {
        return try {
            gson.fromJson(json, AuthResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
