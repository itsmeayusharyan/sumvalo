package com.acuranza.app.repo

import com.acuranza.app.data.prefs.SettingsStore
import com.acuranza.app.finance.Insight
import com.acuranza.app.finance.Severity
import com.acuranza.app.model.Money
import com.acuranza.app.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The in-app notification center: everything that would be worth a system notification, derived
 * from the same local data the daily worker uses — card bills (reminders and upcoming dues),
 * important insights and transactions waiting for review. Read state lives in [SettingsStore].
 * Purely offline; nothing here posts or fetches anything.
 */
data class NotificationItem(
    /** Stable id, so the read state survives list rebuilds. */
    val id: String,
    val kind: Kind,
    val title: String,
    val message: String,
    val whenLabel: String,
    /** Route to open when the item is tapped; null for items with nowhere to go. */
    val route: String?,
    /** Epoch day the item was generated for, newest first in the list. */
    val epochDay: Long,
    val unread: Boolean,
) {
    enum class Kind { BILL, INSIGHT, REVIEW }
}

class NotificationCenter(
    private val bills: BillRepository,
    private val insights: InsightRepository,
    private val transactions: TransactionRepository,
    private val settings: SettingsStore,
) {
    /** Every current notification, unread ones first. */
    fun observeNotifications(today: LocalDate = Time.today()): Flow<List<NotificationItem>> =
        combine(
            bills.observeUnpaid(),
            insights.observeInsights(),
            transactions.observeUnreviewed(),
            settings.settings,
        ) { bills, insights, unreviewed, s ->
            build(bills, insights, unreviewed.size, s.readNotificationIds, today)
        }.flowOn(Dispatchers.Default)

    /** Marks every id in [ids] as read. */
    suspend fun markRead(ids: Collection<String>) = settings.markNotificationsRead(ids)

    companion object {
        private val DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

        /**
         * Pure derivation from live data, so it can be unit-tested: bills due within three days
         * (or overdue) become reminders, later unpaid bills become upcoming dues, alert and
         * warning insights are the important ones, and anything unreviewed is an app alert.
         */
        fun build(
            bills: List<BillItem>,
            insights: List<Insight>,
            unreviewedCount: Int,
            readIds: Set<String>,
            today: LocalDate,
        ): List<NotificationItem> {
            val items = mutableListOf<NotificationItem>()
            bills.forEach { b ->
                val days = b.daysLeft ?: return@forEach
                val card = b.card?.name ?: "Card"
                val due = b.dueDate
                val (title, message) = when {
                    days < 0 -> "$card bill overdue" to "${Money.format(b.bill.totalDue, showPaise = false)} was due ${due?.let { DAY.format(it) } ?: "earlier"}."
                    days == 0L -> "$card bill due today" to "${Money.format(b.bill.totalDue, showPaise = false)} is due today."
                    days <= 3 -> "$card bill due in $days days" to
                        "${Money.format(b.bill.totalDue, showPaise = false)} is due ${due?.let { "on ${DAY.format(it)}" } ?: "soon"}."
                    else -> "$card bill due ${due?.let { DAY.format(it) } ?: ""}".trim() to
                        "${Money.format(b.bill.totalDue, showPaise = false)} due ${due?.let { DAY.format(it) } ?: "later this month"}."
                }
                items += NotificationItem(
                    id = "bill_${b.bill.id}",
                    kind = NotificationItem.Kind.BILL,
                    title = title,
                    message = message,
                    whenLabel = whenLabel(days),
                    route = com.acuranza.app.ui.nav.Routes.BILLS,
                    epochDay = today.toEpochDay(),
                    unread = "bill_${b.bill.id}" !in readIds,
                )
            }
            insights.filter { it.severity == Severity.ALERT || it.severity == Severity.WARNING }.forEach { i ->
                items += NotificationItem(
                    id = "insight_${i.id}",
                    kind = NotificationItem.Kind.INSIGHT,
                    title = i.title,
                    message = i.message,
                    whenLabel = "Today",
                    route = com.acuranza.app.ui.nav.Routes.INSIGHTS,
                    epochDay = today.toEpochDay(),
                    unread = "insight_${i.id}" !in readIds,
                )
            }
            if (unreviewedCount > 0) {
                items += NotificationItem(
                    id = "review_pending",
                    kind = NotificationItem.Kind.REVIEW,
                    title = if (unreviewedCount == 1) "1 transaction to review" else "$unreviewedCount transactions to review",
                    message = "Confirm them so your balances and budgets stay accurate.",
                    whenLabel = "Today",
                    route = com.acuranza.app.ui.nav.Routes.REVIEW,
                    epochDay = today.toEpochDay(),
                    unread = "review_pending" !in readIds,
                )
            }
            return items.sortedWith(
                compareByDescending<NotificationItem> { it.unread }
                    .thenBy { rank(it) }
                    .thenByDescending { it.epochDay },
            )
        }

        /** Overdue bills and alerts surface above routine items. */
        private fun rank(item: NotificationItem): Int = when (item.kind) {
            NotificationItem.Kind.BILL -> if (item.whenLabel.contains("overdue", ignoreCase = true)) 0 else 1
            NotificationItem.Kind.INSIGHT -> 2
            NotificationItem.Kind.REVIEW -> 3
        }

        private fun whenLabel(days: Long): String = when {
            days < 0 -> "${-days}d overdue"
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            else -> "In $days days"
        }
    }
}
