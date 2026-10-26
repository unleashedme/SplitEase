package com.example.splitease.bill

import kotlin.math.abs

/** One line of text found on the bill photo, with its position in pixels. */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Int get() = bottom - top
}

/**
 * Picks the bill total out of the text read from a bill photo.
 *
 * OCR often returns a label ("Grand Total") and its amount ("450.00") as separate lines,
 * so lines are first joined back into the rows they sit on.
 */
object BillAmountParser {

    // Labels for the final amount; they win over a plain "Total"
    private val finalTotalLabel = words(
        "grand total", "net total", "total amount", "amount due", "amount payable", "net payable",
        "total payable", "net amount", "bill amount", "balance due", "total due", "to pay", "bill total"
    )

    // Rows that are never the total, even though some contain the word "total"
    private val notTotalLabel = words(
        "subtotal", "sub total", "sub-total", "total qty", "total quantity", "total items", "total item",
        "no of items", "round off", "roundoff", "rounding", "you saved", "savings", "change", "tendered",
        "cash", "card", "upi", "paid", "gstin", "phone", "mobile", "tel", "invoice no", "bill no", "table", "token"
    )

    // A tax or discount word before "total" makes it a partial total, e.g. "GST Total"
    private val partialTotalPrefix = words(
        "tax", "gst", "cgst", "sgst", "igst", "vat", "cess", "service charge", "discount", "item", "items"
    )

    // Used only when no row is labelled as a total
    private val pricedLineToSkip = words(
        "tax", "taxable", "gst", "cgst", "sgst", "igst", "vat", "cess", "discount", "qty"
    )

    private val totalWord = Regex("""\btotal\b""")
    private val currencyMarker = Regex("""₹|\b(?:rs|inr)\.?(?=\s*\d)""")
    // Dates, times and percentages contain numbers that look like amounts
    private val notAnAmount = Regex("""\d{1,4}[/.-]\d{1,2}[/.-]\d{2,4}|\d{1,2}:\d{2}(?::\d{2})?|\d+(?:\.\d+)?\s*%""")
    // 1,23,456.00 / 1,234.50 / 450 / 450.5, but not part of a longer number or word
    private val amount = Regex("""(?<![\w.,])(\d{1,3}(?:,\d{2,3})+|\d+)(\.\d{1,2})?(?![\w,]|\.\d)""")

    private const val MAX_AMOUNT = 10_000_000.0

    private fun words(vararg phrases: String) =
        Regex(phrases.joinToString("|", prefix = """\b(?:""", postfix = """)\b""") { Regex.escape(it) })

    private data class Candidate(val value: Double, val hasDecimals: Boolean, val hadCurrency: Boolean)

    fun findTotal(lines: List<OcrLine>): Double? {
        val rows = groupIntoRows(lines.filter { it.text.isNotBlank() })

        val labelled = rows.mapNotNull { text ->
            val rank = when {
                finalTotalLabel.containsMatchIn(text) -> 2
                notTotalLabel.containsMatchIn(text) -> return@mapNotNull null
                totalWord.containsMatchIn(text) &&
                        !partialTotalPrefix.containsMatchIn(text.substringBefore("total")) -> 1
                else -> return@mapNotNull null
            }
            // The amount sits at the right end of the row
            amountsIn(text).lastOrNull()?.let { rank to it.value }
        }
        if (labelled.isNotEmpty()) {
            val best = labelled.maxOf { it.first }
            // Several "Total" rows (e.g. before and after tax): the final one is the largest
            return labelled.filter { it.first == best }.maxOf { it.second }
        }

        // No labelled total: fall back to the largest price-looking number on the bill
        return rows
            .filterNot { notTotalLabel.containsMatchIn(it) || pricedLineToSkip.containsMatchIn(it) }
            .flatMap { amountsIn(it) }
            .filter { it.hasDecimals || it.hadCurrency }
            .maxOfOrNull { it.value }
    }

    private fun amountsIn(text: String): List<Candidate> {
        val hadCurrency = currencyMarker.containsMatchIn(text)
        val cleaned = notAnAmount.replace(currencyMarker.replace(text, " "), " ")
        return amount.findAll(cleaned).mapNotNull { match ->
            val whole = match.groupValues[1].replace(",", "")
            // Long digit runs are phone, bill or GST numbers
            if (whole.length > 7) return@mapNotNull null
            val value = (whole + match.groupValues[2]).toDoubleOrNull() ?: return@mapNotNull null
            if (value <= 0 || value >= MAX_AMOUNT) return@mapNotNull null
            Candidate(value, hasDecimals = match.groupValues[2].isNotEmpty(), hadCurrency = hadCurrency)
        }.toList()
    }

    /**
     * Joins lines whose vertical centres are close enough to be on the same printed row,
     * returning each row's text in lower case, left to right.
     */
    private fun groupIntoRows(lines: List<OcrLine>): List<String> {
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in lines.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            val tolerance = maxOf(row?.maxOf { it.height } ?: 0, line.height) * 0.5
            if (row != null && abs(line.centerY - row.map { it.centerY }.average()) <= tolerance) {
                row.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text }.lowercase() }
    }
}
