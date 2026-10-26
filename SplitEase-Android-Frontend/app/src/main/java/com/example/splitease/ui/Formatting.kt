package com.example.splitease.ui

import java.math.BigDecimal
import java.math.RoundingMode

// Plain digits so the amount field can parse it back, e.g. 1234.5 -> "1234.50"
fun formatAmount(amount: Double): String =
    BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString()

/** How every amount is shown in the app, always with 2 decimal places, e.g. "₹1234.50". */
fun rupees(amount: Double): String =
    if (amount < 0) "-₹${formatAmount(-amount)}" else "₹${formatAmount(amount)}"
