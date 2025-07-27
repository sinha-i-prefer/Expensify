package com.example.expenses

import java.util.regex.Pattern

data class Transaction(
    val type: TransactionType,
    val amount: Double
)

enum class TransactionType {
    CREDIT, DEBIT
}

object TransactionParser {
    fun parseTransaction(message: String): Transaction? {
        // Handle SBI-specific format first
        if (message.contains("SBI", ignoreCase = true)) {
            return parseSbiTransaction(message)
        }

        // Generic patterns for other banks
        val creditPatterns = listOf(
            ".*?(?:credited|received|added|deposited).*?(?:rs\\.?|inr\\.?|rupees)\\.?\\s*(\\d+(?:\\.\\d+)?)",
            ".*?rs\\.?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:credited|received|added)",
            ".*?amount\\s*of\\s*rs\\.?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:credited|received)"
        )

        val debitPatterns = listOf(
            ".*?(?:debited|deducted|paid|withdrawn|charged|sent).*?(?:rs\\.?|inr\\.?|rupees)\\.?\\s*(\\d+(?:\\.\\d+)?)",
            ".*?rs\\.?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:debited|deducted|paid)",
            ".*?amount\\s*of\\s*rs\\.?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:debited|deducted)"
        )

        // Check for credit transactions
        for (pattern in creditPatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message)
            if (matcher.find()) {
                val amount = matcher.group(1)?.toDoubleOrNull()
                if (amount != null) {
                    return Transaction(TransactionType.CREDIT, amount)
                }
            }
        }

        // Check for debit transactions
        for (pattern in debitPatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message)
            if (matcher.find()) {
                val amount = matcher.group(1)?.toDoubleOrNull()
                if (amount != null) {
                    return Transaction(TransactionType.DEBIT, amount)
                }
            }
        }

        return null
    }

    private fun parseSbiTransaction(message: String): Transaction? {
        try {
            // Pattern for SBI format: "debited by 1.0" or "credited by 1.0"
            val sbiPattern = Pattern.compile(
                "(debited|credited)\\s*(?:by)?\\s*(?:rs\\.?|inr\\.?)\\s*(\\d+(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
            )

            val matcher = sbiPattern.matcher(message)
            if (matcher.find()) {
                val typeStr = matcher.group(1)?.lowercase()
                val amount = matcher.group(2)?.toDoubleOrNull()

                if (typeStr != null && amount != null) {
                    val type = if (typeStr == "debited") {
                        TransactionType.DEBIT
                    } else {
                        TransactionType.CREDIT
                    }
                    return Transaction(type, amount)
                }
            }
        } catch (e: Exception) {
            // Fall back to generic parsing
        }

        return null
    }
}