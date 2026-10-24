package com.example.splitease.viewmodel

import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.testutil.Fixtures.asha
import com.example.splitease.testutil.Fixtures.meera
import com.example.splitease.testutil.Fixtures.ravi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure split helpers behind the Add Expense dialog. */
class AddExpenseSplitLogicTest {

    private val everyone = listOf(asha, ravi, meera)

    private fun state(
        mode: SplitMode,
        amount: String = "300",
        included: Set<String> = everyone.map { it.userId }.toSet(),
        values: Map<String, String> = emptyMap()
    ) = AddExpenseUiState(
        amount = amount, description = "Dinner", splitMode = mode,
        members = everyone, includedMemberIds = included, splitValues = values
    )

    @Test
    fun equalSplitSendsOnlyIncludedMembersWithoutValues() {
        val s = state(SplitMode.EQUAL, included = setOf(asha.userId, meera.userId))

        assertEquals(
            listOf(SplitEntryDto(asha.userId, null), SplitEntryDto(meera.userId, null)),
            s.splitEntries()
        )
        assertNull(s.splitError())
    }

    @Test
    fun equalSplitPreviewDividesBetweenIncludedMembers() {
        val s = state(SplitMode.EQUAL, included = setOf(asha.userId, ravi.userId))

        assertEquals(150.0, s.previewShare(asha.userId), 0.001)
        assertEquals(150.0, s.previewShare(ravi.userId), 0.001)
        assertEquals(0.0, s.previewShare(meera.userId), 0.001)
    }

    @Test
    fun equalSplitNeedsSomeoneSelected() {
        assertEquals("Select at least one person", state(SplitMode.EQUAL, included = emptySet()).splitError())
    }

    @Test
    fun exactSplitIsValidOnlyWhenAmountsAddUpToTotal() {
        val ok = state(SplitMode.EXACT, values = mapOf(asha.userId to "100", ravi.userId to "150.50", meera.userId to "49.50"))
        val short = state(SplitMode.EXACT, values = mapOf(asha.userId to "100", ravi.userId to "100"))

        assertNull(ok.splitError())
        assertEquals(300.0, ok.splitTotal(), 0.001)
        assertTrue(short.splitError()!!.startsWith("Amounts add up to"))
    }

    @Test
    fun exactSplitSendsEveryMemberAndTreatsBlankAsZero() {
        val s = state(SplitMode.EXACT, values = mapOf(asha.userId to "300"))

        assertEquals(
            listOf(SplitEntryDto(asha.userId, 300.0), SplitEntryDto(ravi.userId, 0.0), SplitEntryDto(meera.userId, 0.0)),
            s.splitEntries()
        )
        assertEquals(300.0, s.previewShare(asha.userId), 0.001)
    }

    @Test
    fun percentageSplitMustAddUpTo100() {
        val ok = state(SplitMode.PERCENTAGE, values = mapOf(asha.userId to "50", ravi.userId to "30", meera.userId to "20"))
        val over = state(SplitMode.PERCENTAGE, values = mapOf(asha.userId to "60", ravi.userId to "60"))

        assertNull(ok.splitError())
        assertEquals(150.0, ok.previewShare(asha.userId), 0.001)
        assertEquals(60.0, ok.previewShare(meera.userId), 0.001)
        assertTrue(over.splitError()!!.startsWith("Percentages add up to"))
    }

    @Test
    fun sharesSplitWeightsEachPerson() {
        val s = state(SplitMode.SHARES, values = mapOf(asha.userId to "2", ravi.userId to "1", meera.userId to "0"))

        assertNull(s.splitError())
        assertEquals(200.0, s.previewShare(asha.userId), 0.001)
        assertEquals(100.0, s.previewShare(ravi.userId), 0.001)
        assertEquals(0.0, s.previewShare(meera.userId), 0.001)
    }

    @Test
    fun sharesSplitNeedsAtLeastOneShare() {
        val s = state(SplitMode.SHARES, values = mapOf(asha.userId to "0"))

        assertEquals("Give at least one person a share", s.splitError())
    }

    @Test
    fun negativeValuesAreRejected() {
        val s = state(SplitMode.EXACT, values = mapOf(asha.userId to "350", ravi.userId to "-50"))

        assertEquals("Values can't be negative", s.splitError())
    }

    @Test
    fun noMembersLoadedMeansNoSplitErrorAndNoEntries() {
        // The backend then splits equally between the whole group
        val s = AddExpenseUiState(amount = "100", description = "x", splitMode = SplitMode.EQUAL)

        assertNull(s.splitError())
        assertTrue(s.splitEntries().isEmpty())
    }
}
