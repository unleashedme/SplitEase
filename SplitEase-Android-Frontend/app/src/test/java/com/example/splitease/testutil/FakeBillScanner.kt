package com.example.splitease.testutil

import com.example.splitease.bill.BillScanner
import com.example.splitease.bill.ScannedBill
import kotlinx.coroutines.CompletableDeferred

/** BillScanner that answers with [result]; replace it to return a different bill, throw, or wait. */
class FakeBillScanner : BillScanner {

    val scannedUris = mutableListOf<String>()
    var lastReadTotal: Boolean? = null

    var result: suspend (String) -> ScannedBill = { ScannedBill(byteArrayOf(1, 2, 3), detectedAmount = 450.0) }

    override suspend fun scan(uri: String, readTotal: Boolean): ScannedBill {
        scannedUris += uri
        lastReadTotal = readTotal
        return result(uri)
    }

    /** Makes the next scans wait until the returned deferred is completed. */
    fun suspendUntilCompleted(bill: ScannedBill): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        result = { gate.await(); bill }
        return gate
    }
}
