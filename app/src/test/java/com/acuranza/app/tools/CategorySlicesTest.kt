package com.acuranza.app.tools

import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The category donut's slice math: top-N + Other, normalised fractions, income excluded. */
class CategorySlicesTest {

    private fun txn(amount: Long, category: String, type: TxnType = TxnType.EXPENSE) = TransactionEntity(
        accountId = 1, type = type, amount = amount, categoryKey = category,
        timestamp = LocalDateTime.of(2026, 10, 15, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        source = TxnSource.MANUAL,
    )

    @Test fun topCategoriesPlusOther() {
        val slices = CategorySlices.of(
            listOf(
                txn(500_00, "food"), txn(300_00, "food"),
                txn(200_00, "transport"), txn(100_00, "bills"),
                txn(80_00, "fun"), txn(60_00, "health"),
                txn(70_00, "income", TxnType.INCOME),
            ),
            categoryNames = mapOf("food" to "Food", "transport" to "Transport", "bills" to "Bills", "fun" to "Fun", "health" to "Health"),
            categoryColors = emptyMap(),
            max = 4,
        )
        assertEquals(5, slices.size) // 4 largest + Other
        assertEquals("Food", slices[0].name)
        assertEquals(800_00, slices[0].amount)
        assertEquals("Other", slices.last().name)
        assertEquals(60_00, slices.last().amount) // health only — fun is in the top 4
        // Fractions normalise to a closed circle.
        assertEquals(1.0, slices.sumOf { it.fraction.toDouble() }, 1e-6)
    }

    @Test fun emptyAndZeroSpending() {
        assertTrue(CategorySlices.of(emptyList(), emptyMap(), emptyMap()).isEmpty())
        assertTrue(CategorySlices.of(listOf(txn(500_00, "food", TxnType.INCOME)), emptyMap(), emptyMap()).isEmpty())
    }
}
