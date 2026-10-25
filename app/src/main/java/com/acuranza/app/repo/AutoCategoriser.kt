package com.acuranza.app.repo

import androidx.room.withTransaction
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.domain.SmsMapping

/** Gives uncategorised imported transactions a real category; see [SmsMapping.recategorised]. */
object AutoCategoriser {
    /** Returns how many transactions changed category. */
    suspend fun run(db: SumvaloDatabase): Int {
        val rules = db.merchantRules().all().associate { it.merchant to it.categoryKey }
        // Without taught rules nothing can be recategorised — skip the per-launch table scan.
        if (rules.isEmpty()) return 0
        // Bounded so a large backlog drains across launches instead of one long cold start.
        val updates = db.transactions().recategoriseCandidates().take(200)
            .mapNotNull { SmsMapping.recategorised(it, rules) }
        if (updates.isEmpty()) return 0
        // One transaction, so screens recompute once rather than once per row.
        db.withTransaction { updates.forEach { db.transactions().update(it) } }
        return updates.size
    }
}
