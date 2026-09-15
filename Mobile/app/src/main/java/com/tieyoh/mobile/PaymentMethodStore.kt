package com.tieyoh.mobile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class PaymentMethodType { BANK_ACCOUNT, DEBIT_CARD, CREDIT_CARD }

data class SavedPaymentMethod(
    val type: PaymentMethodType,
    val label: String,
    val last4: String
)

// Stores only masked summaries locally -- never the full account or card
// number. There's no real payment processor wired up yet (no Stripe/Plaid
// integration), so this is a placeholder the same way ProfilePhotoStore is:
// enough to build and demo the onboarding flow, not a real payments
// integration. Wiring this to a real processor + backend endpoint is a
// later step.
object PaymentMethodStore {

    private const val PREFS_NAME = "tieyoh_payment_methods"
    private const val KEY_METHODS = "methods"

    private val gson = Gson()

    fun load(context: Context): List<SavedPaymentMethod> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_METHODS, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<SavedPaymentMethod>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, method: SavedPaymentMethod) {
        val updated = load(context) + method
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_METHODS, gson.toJson(updated))
            .apply()
    }

    fun remove(context: Context, method: SavedPaymentMethod) {
        val updated = load(context) - method
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_METHODS, gson.toJson(updated))
            .apply()
    }
}
