package com.expensetracker.app

import android.content.Context
import java.util.Locale

object CurrencyManager {
    private const val PREFS_NAME = "expense_tracker_preferences"
    private const val KEY_CURRENCY_SYMBOL = "pref_currency_symbol"
    private const val KEY_GROUP_BY_DATE = "pref_group_by_date"

    const val CURRENCY_DOLLAR = "$"
    const val CURRENCY_RUPEE = "₹"

    fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENCY_SYMBOL, CURRENCY_DOLLAR) ?: CURRENCY_DOLLAR
    }

    fun setCurrencySymbol(context: Context, symbol: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun isGroupByDateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GROUP_BY_DATE, true)
    }

    fun setGroupByDateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GROUP_BY_DATE, enabled).apply()
    }

    fun formatAmount(context: Context, amount: Double): String {
        val symbol = getCurrencySymbol(context)
        return formatAmountWithSymbol(symbol, amount)
    }

    fun formatAmountWithSymbol(symbol: String, amount: Double): String {
        return String.format(Locale.US, "%s%.2f", symbol, amount)
    }

    fun formatExpenseAmount(context: Context, amount: Double): String {
        val symbol = getCurrencySymbol(context)
        return String.format(Locale.US, "-%s%.2f", symbol, amount)
    }
}
