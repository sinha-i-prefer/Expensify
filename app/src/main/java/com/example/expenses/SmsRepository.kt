package com.example.expenses

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsRepository {
    private val _transactions = MutableSharedFlow<Transaction>()
    val transactions = _transactions.asSharedFlow()

    fun addTransaction(transaction: Transaction) {
        // This will be called from SmsReceiver
        _transactions.tryEmit(transaction)
    }
}