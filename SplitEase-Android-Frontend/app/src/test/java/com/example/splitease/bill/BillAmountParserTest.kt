package com.example.splitease.bill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillAmountParserTest {

    /**
     * Lays bill rows out top to bottom the way OCR reports them: each row's label and amount
     * come back as separate lines at the same height.
     */
    private fun bill(vararg rows: Pair<String, String?>): List<OcrLine> =
        rows.flatMapIndexed { i, (label, amount) ->
            val top = i * 40
            listOfNotNull(
                OcrLine(label, left = 10, top = top, right = 200, bottom = top + 24),
                amount?.let { OcrLine(it, left = 400, top = top + 2, right = 480, bottom = top + 26) }
            )
        }

    private fun total(vararg rows: Pair<String, String?>) = BillAmountParser.findTotal(bill(*rows))

    @Test
    fun restaurantBillUsesGrandTotalNotSubtotalOrTax() {
        val amount = total(
            "CAFE KAVERI" to null,
            "GSTIN 29ABCDE1234F1Z5" to null,
            "Date: 12/03/2026  20:45" to null,
            "Masala Dosa" to "120.00",
            "Filter Coffee x2" to "80.00",
            "Sub Total" to "200.00",
            "CGST 2.5%" to "5.00",
            "SGST 2.5%" to "5.00",
            "Grand Total" to "210.00",
            "Cash" to "500.00",
            "Change" to "290.00"
        )

        assertEquals(210.0, amount!!, 0.0)
    }

    @Test
    fun plainTotalRowIsUsed() {
        assertEquals(
            450.0,
            total("Item A" to "300.00", "Item B" to "150.00", "TOTAL" to "450.00", "Thank you!" to null)!!,
            0.0
        )
    }

    @Test
    fun finalTotalLabelBeatsPlainTotal() {
        val amount = total(
            "Total" to "1,000.00",
            "Discount" to "100.00",
            "Net Amount" to "900.00"
        )

        assertEquals(900.0, amount!!, 0.0)
    }

    @Test
    fun largestOfSeveralPlainTotalsIsTheFinalOne() {
        val amount = total(
            "Total" to "500.00",
            "Service charge 10%" to "50.00",
            "Total" to "550.00"
        )

        assertEquals(550.0, amount!!, 0.0)
    }

    @Test
    fun partialTotalsAreSkipped() {
        val amount = total(
            "Total Qty" to "4",
            "Tax Total" to "18.00",
            "Total incl. GST" to "118.00"
        )

        assertEquals(118.0, amount!!, 0.0)
    }

    @Test
    fun understandsIndianDigitGroupingAndCurrencyMarks() {
        assertEquals(123456.5, total("Amount Payable" to "₹1,23,456.50")!!, 0.0)
        assertEquals(1499.0, total("Total" to "Rs. 1,499/-")!!, 0.0)
        assertEquals(250.0, total("Bill Amount" to "INR 250")!!, 0.0)
    }

    @Test
    fun labelAndAmountOnOneLineWork() {
        val lines = listOf(OcrLine("Grand Total: 875.50", 10, 0, 300, 24))

        assertEquals(875.5, BillAmountParser.findTotal(lines)!!, 0.0)
    }

    @Test
    fun linesAreMatchedToTheirOwnRow() {
        // The amounts sit slightly lower than their labels, as on a photo taken at an angle
        val lines = listOf(
            OcrLine("Subtotal", 10, 100, 120, 124),
            OcrLine("Total", 10, 140, 120, 164),
            OcrLine("90.00", 400, 106, 480, 130),
            OcrLine("99.00", 400, 147, 480, 171)
        )

        assertEquals(99.0, BillAmountParser.findTotal(lines)!!, 0.0)
    }

    @Test
    fun phoneNumbersDatesAndPercentagesAreNotAmounts() {
        val amount = total(
            "Ph: 9876543210" to null,
            "Bill No 1234567890" to null,
            "26-10-2026 13:05" to null,
            "Total" to "75.00"
        )

        assertEquals(75.0, amount!!, 0.0)
    }

    @Test
    fun withoutAnyTotalLabelFallsBackToLargestPrice() {
        val amount = total(
            "Paneer Tikka" to "260.00",
            "Naan" to "40.00",
            "CGST" to "750.00", // a misread tax line must not win
            "Thank you, visit again" to null
        )

        assertEquals(260.0, amount!!, 0.0)
    }

    @Test
    fun fallbackIgnoresBareNumbers() {
        assertNull(total("Table 12" to null, "Guests 4" to null, "Order 56" to null))
    }

    @Test
    fun wordsContainingLabelsAreNotMistakenForThem() {
        // "hotel" contains "tel", "Cashew" contains "cash"
        val amount = total(
            "Hotel Sagar" to null,
            "Cashew Curry" to "220.00",
            "Total" to "220.00"
        )

        assertEquals(220.0, amount!!, 0.0)
    }

    @Test
    fun nothingReadableGivesNull() {
        assertNull(BillAmountParser.findTotal(emptyList()))
        assertNull(total("blurry" to null, "photo" to null))
    }
}
