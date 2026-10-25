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
    suspend fun run(db: SumvaloDatabase, sinceMillis: Long? = null): Int {
        val accounts = db.accounts().all()
        val txns = if (sinceMillis != null) db.transactions().since(sinceMillis) else db.transactions().all()
        val pairs = TransferPairing.findPairs(txns, accounts)
        if (pairs.isEmpty()) return 0
        db.withTransaction {
            for (pair in pairs) {
                val (debit, credit) = TransferPairing.asTransfer(pair, accounts)
                db.transactions().update(debit)
                db.transactions().update(credit)
            }
        }
        return pairs.size
    }
}
