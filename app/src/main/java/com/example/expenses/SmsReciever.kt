package com.example.expenses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress
                val body = message.messageBody

                Log.d("SmsReceiver", "Received SMS from $sender: $body")

                // Process the SMS and extract transaction info
                val transaction = TransactionParser.parseTransaction(body)
                if (transaction != null) {
                    // Send to repository
                    SmsRepository.addTransaction(transaction)
                    Log.d("SmsReceiver", "Parsed transaction: ${transaction.type} ₹${transaction.amount}")
                } else {
                    Log.d("SmsReceiver", "No transaction found in message: $body")
                }
            }
        }
    }
}