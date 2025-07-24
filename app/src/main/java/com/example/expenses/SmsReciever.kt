package com.example.expenses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                val messageBody = smsMessage.messageBody
                val sender = smsMessage.originatingAddress

                // Only process messages from SBI or other bank senders
                if (isBankMessage(sender, messageBody)) {
                    coroutineScope.launch {
                        parseSmsForTransaction(context, messageBody)
                    }
                }
            }
        }
    }

    private fun isBankMessage(sender: String?, messageBody: String): Boolean {
        // Check if message is from a bank
        val bankSenders = listOf("SBI", "SBIINB", "HDFCBK", "ICICI", "AXIS", "KOTAK")
        val lowerMessage = messageBody.lowercase()

        return (sender != null && bankSenders.any { sender.contains(it, ignoreCase = true) }) ||
                lowerMessage.contains("debited") ||
                lowerMessage.contains("credited") ||
                lowerMessage.contains("upi") ||
                messageBody.contains("-SBI") // SBI signature
    }

    private suspend fun parseSmsForTransaction(context: Context, message: String) {
        // SBI UPI format: "A/C X5385 debited by 1.0 on date 25Jul25"
        // Also handle: "credited by", "debited with", "credited with"

        val debitPattern = Pattern.compile("debited\\s+(?:by|with)\\s+([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        val creditPattern = Pattern.compile("credited\\s+(?:by|with)\\s+([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)

        var amount: Double? = null
        var isCredit = false

        // Try to match debit pattern first
        var matcher = debitPattern.matcher(message)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
            isCredit = false
        } else {
            // Try credit pattern
            matcher = creditPattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
                isCredit = true
            }
        }

        // If no specific pattern found, try generic amount detection
        if (amount == null) {
            amount = parseGenericAmount(message)
            if (amount != null) {
                isCredit = determineTransactionType(message)
            }
        }

        // Update balance if we found a valid transaction
        if (amount != null && amount > 0) {
            val repository = BalanceRepository(context)
            repository.updateBalance(amount, isCredit)
        }
    }

    private fun parseGenericAmount(message: String): Double? {
        // Fallback patterns for other formats
        val patterns = listOf(
            "(?:Rs|INR)\\.?\\s*([\\d,]+\\.?\\d*)",  // Rs. 500 or INR 500
            "\\b([\\d,]+\\.\\d{2})\\b",              // 500.00 (with decimals)
            "amount\\s+(?:of\\s+)?([\\d,]+\\.?\\d*)", // amount 500 or amount of 500
            "\\b([\\d,]{1,10}\\.?\\d{0,2})\\b"       // Generic number pattern
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull()
                // Only accept reasonable amounts (between 1 and 1,000,000)
                if (amount != null && amount >= 1.0 && amount <= 1000000.0) {
                    return amount
                }
            }
        }
        return null
    }

    private fun determineTransactionType(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Credit indicators
        val creditKeywords = listOf(
            "credited", "received", "added", "deposit", "refund",
            "cashback", "reward", "bonus", "transfer received"
        )

        // Debit indicators
        val debitKeywords = listOf(
            "debited", "spent", "sent", "purchase", "withdrawn",
            "payment", "transfer", "charged", "deducted"
        )

        // Check credit first (more specific)
        if (creditKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        // Check debit
        if (debitKeywords.any { lowerMessage.contains(it) }) {
            return false
        }

        // Default to debit if unclear (safer assumption)
        return false
    }
}