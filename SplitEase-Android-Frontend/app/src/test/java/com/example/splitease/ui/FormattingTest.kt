package com.example.splitease.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun amountsAlwaysHaveTwoDecimalPlaces() {
        assertEquals("₹450.00", rupees(450.0))
        assertEquals("₹33.33", rupees(100.0 / 3))
        assertEquals("₹66.67", rupees(200.0 / 3))
        assertEquals("₹0.10", rupees(0.1))
        assertEquals("₹1234567.50", rupees(1234567.5))
    }

    @Test
    fun negativeAmountsPutTheSignBeforeTheRupeeSymbol() {
        assertEquals("-₹12.50", rupees(-12.5))
    }
}
