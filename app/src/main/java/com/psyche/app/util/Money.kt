package com.psyche.app.util

import java.math.BigDecimal
import java.math.RoundingMode

object Money {
    const val MAX_AMOUNT_CENTS = 99_999_999L
    private const val MINUS = "\u2212"

    fun formatPeso(
        cents: Long,
        dropZeroCents: Boolean = true,
        showPlus: Boolean = false,
        showSymbol: Boolean = true,
    ): String {
        val negative = cents < 0
        val absolute = if (negative) -cents else cents
        val body = grouped(absolute, dropZeroCents)
        val sign = when {
            negative -> MINUS
            showPlus -> "+"
            else -> ""
        }
        val symbol = if (showSymbol) "\u20B1" else ""
        return "$sign$symbol$body"
    }

    fun formatTable(cents: Long, type: TransactionMarker): String {
        val absolute = if (cents < 0) -cents else cents
        val body = grouped(absolute, dropZeroCents = true)
        return when (type) {
            TransactionMarker.EXPENSE -> "$MINUS$body"
            TransactionMarker.INCOME -> "+$body"
            TransactionMarker.TRANSFER -> "\u21C4$body"
        }
    }

    fun grouped(cents: Long, dropZeroCents: Boolean = true): String {
        val absolute = if (cents < 0) -cents else cents
        val pesos = absolute / 100
        val centavos = (absolute % 100).toInt()
        val whole = "%,d".format(java.util.Locale.US, pesos)
        return if (centavos == 0 && dropZeroCents) {
            whole
        } else {
            "$whole.${centavos.toString().padStart(2, '0')}"
        }
    }

    /**
     * Parses an amount typed by the owner. Returns null for empty, zero, negative,
     * malformed, or over-limit input. Values are capped at ₱999,999.99.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .replace("\u20B1", "")
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        if (!Regex("""\d{0,7}(\.\d{0,2})?""").matches(cleaned)) return null
        if (cleaned == "." || cleaned.startsWith(".")) return null
        val decimal = runCatching { BigDecimal(cleaned).setScale(2, RoundingMode.UNNECESSARY) }
            .getOrNull() ?: return null
        val cents = runCatching { decimal.movePointRight(2).longValueExact() }.getOrNull() ?: return null
        return cents.takeIf { it in 1..MAX_AMOUNT_CENTS }
    }

    /** A plain editable value for text fields: 8240 or 8240.50. */
    fun inputValue(cents: Long): String {
        val absolute = if (cents < 0) -cents else cents
        val pesos = absolute / 100
        val centavos = (absolute % 100).toInt()
        return if (centavos == 0) {
            pesos.toString()
        } else {
            "$pesos.${centavos.toString().padStart(2, '0')}"
        }
    }

    fun groupTypedAmount(raw: String): String {
        if (raw.isEmpty()) return "0"
        val parts = raw.split(".", limit = 2)
        val whole = parts[0].ifEmpty { "0" }
        val groupedWhole = whole.toLongOrNull()
            ?.let { "%,d".format(java.util.Locale.US, it) }
            ?: whole
        return if (raw.contains('.')) {
            val decimals = parts.getOrElse(1) { "" }
            "$groupedWhole.$decimals"
        } else {
            groupedWhole
        }
    }

    /** Returns the raw keypad value with at most two decimal places and six whole digits. */
    fun appendAmountKey(current: String, key: Char): String {
        if (key.isDigit()) {
            val parts = current.split(".", limit = 2)
            val whole = parts[0]
            val decimal = parts.getOrNull(1)
            if (decimal == null) {
                if (whole.length >= 6) return current
                return when {
                    whole == "0" -> key.toString()
                    whole.isEmpty() -> key.toString()
                    else -> whole + key
                }
            }
            if (decimal.length >= 2) return current
            return current + key
        }
        if (key == '.' && !current.contains('.')) {
            return if (current.isEmpty()) "0." else "$current."
        }
        return current
    }

    fun backspace(current: String): String = current.dropLast(1)
}

enum class TransactionMarker { EXPENSE, INCOME, TRANSFER }
