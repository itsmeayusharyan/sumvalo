package com.acuranza.app.ui.home

import com.acuranza.app.data.NetWorthSnapshotEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The reference-style trend chart's data: hour labels, balance deltas per range, live today point. */
class HomeChartsTrendTest {

    private val today = LocalDate.of(2026, 10, 14)
    private fun rupees(r: Long) = r * 100

    private fun txn(id: Long, type: TxnType, rupees: Long, hour: Int, cat: String = "food") =
        TransactionEntity(
            id = id, accountId = 1, type = type, amount = rupees(rupees), categoryKey = cat,
            merchant = "Merchant", timestamp = Time.startOfDay(today) + hour * 3_600_000L,
            source = TxnSource.MANUAL,
        )

    private fun snap(day: LocalDate, totalRupees: Long) =
        NetWorthSnapshotEntity(day.toEpochDay(), 0, 0, rupees(totalRupees))

    private fun build(txns: List<TransactionEntity>, history: List<NetWorthSnapshotEntity>) =
        HomeCharts.build(txns, Period.containing(today), today, history, nowHour = 14)

    @Test
    fun d1ShowsHourlyNetWorthWithOnlyBalanceMovingTxns() {
        // Yesterday's snapshot ₹1,000. Today: −100 spent, +400 income, a transfer (no effect),
        // a suspicious debit (held, no effect). Now 14:00.
        val charts = build(
            txns = listOf(
                txn(1, TxnType.EXPENSE, 100, 9),
                txn(2, TxnType.INCOME, 400, 10, cat = "salary"),
                txn(3, TxnType.TRANSFER, 200, 11),
                txn(4, TxnType.EXPENSE, 500, 12).copy(suspicious = true),
            ),
            history = listOf(snap(today.minusDays(1), 1_000)),
        )
        val points = charts.trendPoints(TrendRange.D1, today)

        assertEquals(listOf("12 AM", "9 AM", "10 AM", "2 PM"), points.map { it.label })
        assertEquals(listOf(rupees(1_000), rupees(900), rupees(1_300), rupees(1_300)), points.map { it.value })
    }

    @Test
    fun d1WithoutABaseSnapshotIsEmpty() {
        val charts = build(txns = listOf(txn(1, TxnType.EXPENSE, 100, 9)), history = emptyList())
        assertTrue(charts.trendPoints(TrendRange.D1, today).isEmpty())
    }

    @Test
    fun d1WithNoFlowTodayIsFlatThroughNow() {
        val charts = build(txns = emptyList(), history = listOf(snap(today.minusDays(1), 1_000)))
        val points = charts.trendPoints(TrendRange.D1, today)
        assertEquals(listOf("12 AM", "2 PM"), points.map { it.label })
        assertEquals(listOf(rupees(1_000), rupees(1_000)), points.map { it.value })
    }

    @Test
    fun weeklyPointsAreDailyWithTodayAppendedLive() {
        // History ends two days ago, so the chart must extend to today from the hourly flow.
        val history = (1..10).map { snap(today.minusDays(it.toLong()), 1_000L + it) }
        val charts = build(txns = listOf(txn(1, TxnType.EXPENSE, 250, 9)), history = history)

        val week = charts.trendPoints(TrendRange.W1, today)
        assertEquals(7, week.size) // six daily snapshots (1..6 days ago) + today, appended live
        assertEquals("14 OCT", week.last().label)
        assertEquals(rupees(1_001 - 250), week.last().value) // yesterday's ₹1,001 snapshot minus today's ₹250 spend

        val month = charts.trendPoints(TrendRange.M1, today)
        assertEquals(11, month.size) // ten snapshots + today
    }

    @Test
    fun yearAndAllLabelMonthsAndIncludeEverySnapshot() {
        val history = (0 until 400).map { snap(today.minusDays((400 - it).toLong()), 1_000L + it) }
        val charts = build(txns = emptyList(), history = history)

        val year = charts.trendPoints(TrendRange.Y1, today)
        assertEquals(365, year.size) // 364 daily snapshots (1..364 days ago) + the live today point
        assertEquals("OCT '26", year.last().label)
        assertTrue(year.last().value == todayHourlyLast(charts))

        val all = charts.trendPoints(TrendRange.ALL, today)
        assertEquals(401, all.size) // the full history + today
        assertTrue(all.last().value == todayHourlyLast(charts))
    }

    @Test
    fun noLiveAppendWhenHistoryAlreadyEndsToday() {
        val history = (0..10).map { snap(today.minusDays((10 - it).toLong()), 1_000L + it) }
        val charts = build(txns = listOf(txn(1, TxnType.EXPENSE, 100, 9)), history = history)
        val week = charts.trendPoints(TrendRange.W1, today)
        assertEquals(7, week.size) // today's snapshot is present, so no extra point
        assertEquals(rupees(1_010), week.last().value)
    }

    private fun todayHourlyLast(charts: HomeCharts) = charts.todayHourly.last().second
}
