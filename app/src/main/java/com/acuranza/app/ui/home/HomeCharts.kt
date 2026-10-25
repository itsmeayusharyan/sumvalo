package com.acuranza.app.ui.home

import com.acuranza.app.data.NetWorthSnapshotEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.designsystem.components.BarGroup
import com.acuranza.app.designsystem.components.ChartPoint
import com.acuranza.app.domain.MoneyIn
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/** Selectable windows for the net worth trend chart. */
enum class TrendRange(val days: Int?) { D1(1), W1(7), M1(30), Y1(365), ALL(null) }

/** Everything the Home charts draw, computed off the main thread from transactions and history. */
data class HomeCharts(
    /** Cumulative spend per elapsed day of the current period. */
    val paceCurrent: List<Paise> = emptyList(),
    /** Cumulative spend per day of the previous period. */
    val pacePrevious: List<Paise> = emptyList(),
    val periodDays: Int = 30,
    val projected: Paise? = null,
    val sixMonths: List<BarGroup> = emptyList(),
    val savingsRates: List<String> = emptyList(),
    /** Weeks (oldest first) × 7 days (Monday first); null for days after today. */
    val heatWeeks: List<List<Paise?>> = emptyList(),
    val heatLabels: List<List<String>> = emptyList(),
    /** Net worth by day, oldest first. */
    val netWorth: List<Pair<LocalDate, Paise>> = emptyList(),
    /** Today's net worth by hour (0-based, oldest first): last snapshot plus today's confirmed flow. */
    val todayHourly: List<Pair<Int, Paise>> = emptyList(),
) {
    val hasPace: Boolean get() = paceCurrent.any { it > 0 } || pacePrevious.any { it > 0 }
    val hasSixMonths: Boolean get() = sixMonths.count { it.income > 0 || it.expense > 0 } >= 2
    val hasHeat: Boolean get() = heatWeeks.flatten().any { (it ?: 0L) > 0 }

    /**
     * Net worth points for a [TrendRange]: the daily history inside the window, plus a live point
     * for today from [todayHourly] when the daily snapshot hasn't been recorded yet.
     */
    fun trendPoints(range: TrendRange, today: LocalDate = Time.today()): List<ChartPoint> {
        if (range == TrendRange.D1) return todayHourly.map { (h, v) -> ChartPoint(hourLabel(h), v) }
        val from = range.days?.let { today.minusDays(it - 1L) } ?: LocalDate.MIN
        val window = netWorth.filter { !it.first.isBefore(from) && !it.first.isAfter(today) }
        val points = window.map { (d, v) -> ChartPoint(dayLabel(d, range), v) }.toMutableList()
        val lastDay = window.lastOrNull()?.first
        if (todayHourly.isNotEmpty() && (lastDay == null || lastDay.isBefore(today))) {
            points += ChartPoint(dayLabel(today, range), todayHourly.last().second)
        }
        return points
    }

    /** Hours as "12 AM", "9 AM", "2 PM", like the reference chart's time label. */
    private fun hourLabel(hour: Int): String {
        val h = hour % 12
        return "${if (h == 0) 12 else h} ${if (hour < 12) "AM" else "PM"}"
    }

    /** Days as "26 MAY" for 1W/1M, months as "MAY '25" for 1Y/ALL. */
    private fun dayLabel(day: LocalDate, range: TrendRange): String {
        val month = day.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)
        return when (range) {
            TrendRange.Y1, TrendRange.ALL -> "$month '${day.year % 100}"
            else -> "${day.dayOfMonth} $month"
        }
    }

    companion object {
        const val HEAT_WEEKS = 5

        fun build(
            transactions: List<TransactionEntity>,
            period: Period,
            today: LocalDate,
            history: List<NetWorthSnapshotEntity> = emptyList(),
            nowHour: Int = java.time.LocalTime.now().hour,
        ): HomeCharts {
            val counted = transactions.filter { SmsMapping.countsInStats(it) }
            val expenseByDay = counted.filter { it.type == TxnType.EXPENSE }
                .groupBy { Time.toDate(it.timestamp) }.mapValues { (_, t) -> t.sumOf { it.amount } }
            val backByDay = counted.filter(MoneyIn::isGivenBack)
                .groupBy { Time.toDate(it.timestamp) }.mapValues { (_, t) -> t.sumOf { it.amount } }

            // Running spending for the period, with refunds and cashback taken off on the day they arrive.
            fun cumulative(p: Period, upTo: LocalDate): List<Paise> {
                var acc = 0L
                val days = generateSequence(p.start) { it.plusDays(1) }.takeWhile { !it.isAfter(upTo) && it.isBefore(p.endExclusive) }
                return days.map { d -> acc += (expenseByDay[d] ?: 0L) - (backByDay[d] ?: 0L); acc.coerceAtLeast(0) }.toList()
            }

            val current = cumulative(period, today)
            val previousPeriod = period.previous()
            val previous = cumulative(previousPeriod, previousPeriod.lastDay)
            val projected = current.lastOrNull()?.takeIf { current.size >= 3 }?.let { spent ->
                (spent.toDouble() / current.size * period.lengthDays).toLong()
            }

            val periods = (5 downTo 0).map { back -> (0 until back).fold(period) { p, _ -> p.previous() } }
            val bars = periods.map { p ->
                val inP = counted.filter { Time.toDate(it.timestamp) in p }
                BarGroup(
                    p.label.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    inP.filter(MoneyIn::isEarned).sumOf { it.amount },
                    MoneyIn.netSpent(inP.filter { it.type == TxnType.EXPENSE }, inP.filter(MoneyIn::isGivenBack)),
                )
            }
            val rates = bars.map { b -> if (b.income > 0) "${((b.income - b.expense) * 100.0 / b.income).roundToInt()}%" else "–" }

            val firstMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((HEAT_WEEKS - 1).toLong())
            val weeks = (0 until HEAT_WEEKS).map { w ->
                (0 until 7).map { d -> firstMonday.plusDays(w * 7L + d) }
            }
            val heat = weeks.map { week -> week.map { d -> if (d.isAfter(today)) null else expenseByDay[d] ?: 0L } }
            val labels = weeks.map { week ->
                week.map { d ->
                    "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
                }
            }

            // Today's net worth per hour: the last recorded snapshot plus the balance effect of
            // today's confirmed transactions (expenses out, income and refunds in, transfers nothing).
            val dayStart = Time.startOfDay(today)
            val base = history.filter { it.epochDay < today.toEpochDay() }.maxByOrNull { it.epochDay }
            val hourlyToday = if (base == null) {
                emptyList()
            } else {
                val deltas = transactions.asSequence()
                    .filter { !it.suspicious && !it.excludeFromStats && it.type != TxnType.TRANSFER && Time.toDate(it.timestamp) == today }
                    .groupBy { ((it.timestamp - dayStart) / 3_600_000L).toInt().coerceIn(0, 23) }
                    .mapValues { (_, txns) -> txns.sumOf { t -> if (t.type == TxnType.EXPENSE) -t.amount else t.amount } }
                var acc = base.total
                val hourly = mutableListOf(0 to acc)
                deltas.toSortedMap().forEach { (h, d) -> acc += d; hourly += h to acc }
                if (nowHour > hourly.last().first) hourly += nowHour.coerceIn(0, 23) to acc
                hourly
            }

            return HomeCharts(
                paceCurrent = current,
                pacePrevious = previous,
                periodDays = period.lengthDays,
                projected = projected,
                sixMonths = bars,
                savingsRates = rates,
                heatWeeks = heat,
                heatLabels = labels,
                netWorth = history.sortedBy { it.epochDay }.map { LocalDate.ofEpochDay(it.epochDay) to it.total },
                todayHourly = hourlyToday,
            )
        }
    }
}
