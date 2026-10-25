package com.acuranza.app.tools

import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import com.acuranza.app.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * The pure widget math: savings rate, budget bar and goal progress — including the edge cases the
 * spec calls out (negative savings, zero activity, missing targets). The transaction pipeline
 * itself (countsInStats, netSpent) is covered by DigestAndWidgetTest/MoneyInTest.
 */
class WidgetFiguresTest {

    private val period: Period = Period.containing(
        LocalDate.of(2026, 10, 22),
        com.acuranza.app.finance.PayCycle(day = 1),
    )

    private fun txn(type: TxnType, amount: Long) = TransactionEntity(
        accountId = 1, type = type, amount = amount, categoryKey = "x",
        timestamp = LocalDateTime.of(2026, 10, 15, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
    )

    @Test fun figuresMatchAppDefinitions() {
        val f = WidgetFigures.of(period, listOf(txn(TxnType.INCOME, 50_000_00), txn(TxnType.EXPENSE, 12_000_00)), null)
        assertEquals(50_000_00, f.income)
        assertEquals(12_000_00, f.spent)
        assertEquals(38_000_00, f.saved)
    }

    @Test fun savingsRateClampsAndHandlesZeroIncome() {
        // 38_000 saved of 50_000 income -> 76%
        val f = WidgetFigures.of(period, listOf(txn(TxnType.INCOME, 50_000_00), txn(TxnType.EXPENSE, 12_000_00)), null)
        assertEquals(76, f.ratePercent)
        // Spending more than income: the rate clamps to 0, the saved figure keeps its negative sign.
        val negative = WidgetFigures.of(period, listOf(txn(TxnType.INCOME, 10_000_00), txn(TxnType.EXPENSE, 12_000_00)), null)
        assertTrue(negative.saved < 0)
        assertEquals(0, negative.ratePercent)
        // Zero income: no meaningful rate.
        val none = WidgetFigures.of(period, emptyList(), null)
        assertEquals(0, none.income)
        assertNull(none.ratePercent)
    }

    @Test fun barPercentUsesBudgetWhenPresent() {
        // Spent 12_000 against a 24_000 budget -> 50%, regardless of income.
        val f = WidgetFigures.of(
            period,
            listOf(txn(TxnType.INCOME, 50_000_00), txn(TxnType.EXPENSE, 12_000_00)),
            24_000_00,
        )
        assertEquals(50, f.barPercent)
        // No budget: the bar becomes the share of income spent.
        val noBudget = WidgetFigures.of(period, listOf(txn(TxnType.INCOME, 50_000_00), txn(TxnType.EXPENSE, 12_000_00)), null)
        assertEquals(24, noBudget.barPercent)
    }

    @Test fun goalProgressMath() {
        // Null or zero target -> no goal configured.
        assertNull(GoalProgress.of(10_00, null))
        assertNull(GoalProgress.of(10_00, 0))
        val g = GoalProgress.of(30_000_00, 40_000_00)!!
        assertEquals(75, g.percent)
        assertEquals(10_000_00, g.remaining)
        // Oversaved clamps the bar and zeroes the remainder.
        val done = GoalProgress.of(45_000_00, 40_000_00)!!
        assertEquals(100, done.percent)
        assertEquals(0, done.remaining)
    }
}
