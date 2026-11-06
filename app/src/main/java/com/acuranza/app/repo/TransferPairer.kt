package com.acuranza.app.repo

import androidx.room.withTransaction
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.domain.TransferPairing

/** Applies [TransferPairing] to the stored transactions; run after imports and once on upgrade. */
object TransferPairer {
    /**
     * Returns how many self-transfers were found. With [sinceMillis], only transactions from then on are
     * considered (imports pass the window around the new messages instead of scanning everything).
     */
    suspend fun run(db: SumvaloDatabase, sinceMillis: Long? = null): Int = db.withTransaction {
        // Read inside the transaction so the updates carry the rows as they are now, not a stale snapshot.
        val accounts = db.accounts().all()
        val txns = if (sinceMillis != null) db.transactions().since(sinceMillis) else db.transactions().all()
        val pairs = TransferPairing.findPairs(txns, accounts)
        for (pair in pairs) {
            val (debit, credit) = TransferPairing.asTransfer(pair, accounts)
            db.transactions().update(debit)
            db.transactions().update(credit)
        }
        pairs.size
    }
}
