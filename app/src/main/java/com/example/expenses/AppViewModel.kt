package com.example.expenses

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Define DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "balance_prefs")

// Repository class to handle data operations
class BalanceRepository(private val context: Context) {
    companion object {
        val BALANCE_KEY = doublePreferencesKey("current_balance")
    }

    val balanceFlow: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[BALANCE_KEY]
        }

    suspend fun updateBalance(amount: Double, isCredit: Boolean) {
        context.dataStore.edit { preferences ->
            val currentBalance = preferences[BALANCE_KEY] ?: 0.0
            val newBalance = if (isCredit) currentBalance + amount else currentBalance - amount
            preferences[BALANCE_KEY] = newBalance
        }
    }

    suspend fun setInitialBalance(balance: Double) {
        context.dataStore.edit { preferences ->
            preferences[BALANCE_KEY] = balance
        }
    }
}

// ViewModel to hold and manage UI state
class BalanceViewModel(private val repository: BalanceRepository) : ViewModel() {

    // Publicly exposed flow for the UI to observe
    val balance: Flow<Double?> = repository.balanceFlow

    fun setInitialBalance(initialBalance: Double) {
        viewModelScope.launch {
            repository.setInitialBalance(initialBalance)
        }
    }

    // Factory to create ViewModel instance with dependencies
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return BalanceViewModel(
                    BalanceRepository(application.applicationContext)
                ) as T
            }
        }
    }
}
