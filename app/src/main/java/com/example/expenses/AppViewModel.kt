package com.example.expenses

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "balance_prefs")

class BalanceRepository(private val context: Context) {
    companion object {
        val BALANCE_KEY = doublePreferencesKey("current_balance")
    }

    val balanceFlow: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[BALANCE_KEY]
        }

    suspend fun updateBalanceWithTransaction(transaction: Transaction) {
        context.dataStore.edit { preferences ->
            val currentBalance = preferences[BALANCE_KEY] ?: 0.0
            val newBalance = when (transaction.type) {
                TransactionType.CREDIT -> currentBalance + transaction.amount
                TransactionType.DEBIT -> currentBalance - transaction.amount
            }
            preferences[BALANCE_KEY] = newBalance
        }
        // Update widget when balance changes
        updateWidgets()
    }

    suspend fun setInitialBalance(balance: Double) {
        context.dataStore.edit { preferences ->
            preferences[BALANCE_KEY] = balance
        }
        // Update widget when initial balance is set
        updateWidgets()
    }

    private fun updateWidgets() {
        // Launch a coroutine to update widgets
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val glanceAppWidgetManager = GlanceAppWidgetManager(context)
                val widget = BalanceWidget()
                widget.updateAll(context)
            } catch (e: Exception) {
                // Handle exception silently
            }
        }
    }
}