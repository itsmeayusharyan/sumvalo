package com.acuranza.app.repo

import com.acuranza.app.data.AccountEntity
import com.acuranza.app.data.CardBillEntity
import com.acuranza.app.finance.Insight
import com.acuranza.app.finance.Severity
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.TxnSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The notification list is a pure derivation from live data, so its ordering, filtering and
 * read-state behaviour are unit-tested directly against [NotificationCenter.build].
 */
class NotificationCenterTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 19)

    private fun bill(id: Long, dueInDays: Long, paise: Long = 12_345_00) = BillItem(
        bill = CardBillEntity(
            id = id,
            accountId = 1,
            totalDue = paise,
            dueDate = today.plusDays(dueInDays).toEpochDay(),
            source = TxnSource.MANUAL,
            fingerprint = "f$id",
        ),
        card = AccountEntity(
            id = 1,
            name = "Axis Card",
            type = AccountType.CREDIT_CARD,
            bank = "Axis",
        ),
        daysLeft = dueInDays,
    )

    private fun insight(id: String, severity: Severity) = Insight(
        id = id,
        severity = severity,
        title = "T $id",
        message = "M $id",
    )

    // ---------------------------------------------------------------- ordering

    @Test
    fun `overdue bills surface above upcoming ones`() {
        val items = NotificationCenter.build(
            bills = listOf(bill(1, dueInDays = 5), bill(2, dueInDays = -2)),
            insights = emptyList(),
            unreviewedCount = 0,
            readIds = emptySet(),
            today = today,
        )
        assertEquals(listOf("bill_2", "bill_1"), items.map { it.id })
        assertTrue(items.first().title.contains("overdue"))
        assertTrue(items.first().whenLabel.contains("overdue"))
    }

    @Test
    fun `insights and review come after bills and review is filtered when nothing pending`() {
        val items = NotificationCenter.build(
            bills = listOf(bill(1, dueInDays = 2)),
            insights = listOf(insight("a", Severity.ALERT)),
            unreviewedCount = 3,
            readIds = emptySet(),
            today = today,
        )
        assertEquals(listOf("bill_1", "insight_a", "review_pending"), items.map { it.id })
        assertEquals("3 transactions to review", items.last().title)

        val none = NotificationCenter.build(
            bills = emptyList(),
            insights = emptyList(),
            unreviewedCount = 0,
            readIds = emptySet(),
            today = today,
        )
        assertTrue(none.isEmpty())
    }

    // ---------------------------------------------------------------- filtering & wording

    @Test
    fun `only alert and warning insights become notifications`() {
        val items = NotificationCenter.build(
            bills = emptyList(),
            insights = listOf(
                insight("alert", Severity.ALERT),
                insight("warn", Severity.WARNING),
                insight("tip", Severity.TIP),
                insight("good", Severity.GOOD),
            ),
            unreviewedCount = 0,
            readIds = emptySet(),
            today = today,
        )
        assertEquals(listOf("insight_alert", "insight_warn"), items.map { it.id })
    }

    @Test
    fun `due-today and due-soon wordings`() {
        val items = NotificationCenter.build(
            bills = listOf(bill(1, dueInDays = 0), bill(2, dueInDays = 1), bill(3, dueInDays = 4)),
            insights = emptyList(),
            unreviewedCount = 0,
            readIds = emptySet(),
            today = today,
        )
        // Bills due sooner rank first within the same group.
        assertEquals(listOf("bill_1", "bill_2", "bill_3"), items.map { it.id })
        assertEquals("Today", items[0].whenLabel)
        assertEquals("Tomorrow", items[1].whenLabel)
        assertEquals("In 4 days", items[2].whenLabel)
        assertTrue(items[0].message.contains("due today"))
    }

    // ---------------------------------------------------------------- read state

    @Test
    fun `read items keep their place but lose the unread flag`() {
        val items = NotificationCenter.build(
            bills = listOf(bill(1, dueInDays = -1), bill(2, dueInDays = 3)),
            insights = listOf(insight("a", Severity.ALERT)),
            unreviewedCount = 1,
            readIds = setOf("bill_1", "insight_a"),
            today = today,
        )
        val byId = items.associateBy { it.id }
        assertFalse(byId.getValue("bill_1").unread)
        assertFalse(byId.getValue("insight_a").unread)
        assertTrue(byId.getValue("bill_2").unread)
        assertTrue(byId.getValue("review_pending").unread)
        // Unread items lead: bill_2 and review_pending before the read overdue bill.
        assertEquals(
            listOf("bill_2", "review_pending", "bill_1", "insight_a"),
            items.map { it.id },
        )
    }
}
