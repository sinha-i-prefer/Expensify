package com.example.expenses

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BalanceViewModel : ViewModel() {
    private val _balance = MutableStateFlow<Double?>(null)
    val balance: StateFlow<Double?> = _balance.asStateFlow()

    init {
        // Listen for new transactions
        viewModelScope.launch {
            SmsRepository.transactions.collect { transaction ->
                updateBalance(transaction)
            }
        }
    }

    fun setInitialBalance(amount: Double) {
        _balance.value = amount
        Log.d("BalanceViewModel", "Initial balance set to: ₹$amount")
    }

    private fun updateBalance(transaction: Transaction) {
        val currentBalance = _balance.value ?: return // Don't process if no initial balance

        val newBalance = when (transaction.type) {
            TransactionType.CREDIT -> currentBalance + transaction.amount
            TransactionType.DEBIT -> currentBalance - transaction.amount
        }

        _balance.value = newBalance
        Log.d("BalanceViewModel", "Balance updated: ₹$currentBalance ${if (transaction.type == TransactionType.CREDIT) "+" else "-"} ₹${transaction.amount} = ₹$newBalance")
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                BalanceViewModel()
            }
        }
    }
}