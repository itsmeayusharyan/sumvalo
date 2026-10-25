package com.acuranza.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import com.acuranza.app.model.TxnChannel
import com.acuranza.app.model.TxnType
import com.acuranza.app.sms.SmsConfidence

/**
 * One completed import run: the first import, a manual refresh or one automatic scan.
 * Batches are the undo unit — undoing removes exactly the transactions the batch created
 * and never anything that existed before it.
 */
@Entity(tableName = "sms_import_batches")
data class SmsImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Wall-clock time the import was confirmed. */
    val importedAt: Long,
    /** How many transactions the batch created. */
    val txnCount: Int,
    /** "First import", "Manual refresh" or "Automatic scan". */
    val label: String,
    /** Set when the user undid the batch; rows and transactions are already gone. */
    val undone: Boolean = false,
)

/**
 * What Sumvalo keeps about one imported bank SMS. Privacy rule: the message body itself is NEVER
 * stored — only the extracted fields, the sender id and the parser's fingerprint (a hash). That is
 * enough to list, re-edit, delete or re-scan without retaining the raw text.
 */
@Entity(
    tableName = "sms_imports",
    foreignKeys = [
        ForeignKey(entity = SmsImportBatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index("batchId"),
        Index("transactionId"),
    ],
)
data class SmsImportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: Long,
    /** The transaction this import created; null while a review-screen candidate is not yet saved. */
    val transactionId: Long? = null,
    /** The parser's dedupe key: scanning the same message twice imports it once. */
    val fingerprint: String,
    /** Sender id as it appeared (for example "JD-HDFCBK"); the body is not retained. */
    val sender: String,
    /** Bank display name the parser detected. */
    val bank: String,
    /** Last four digits of the account/card the message was about, when present. */
    val accountLast4: String?,
    val type: TxnType,
    val amount: Long,
    val merchant: String?,
    /** Epoch millis of the transaction as parsed. */
    val timestamp: Long,
    val channel: TxnChannel,
    val confidence: SmsConfidence,
    /** The parser's own category for the message (e.g. "transfer" for a card repayment); used at confirm. */
    val suggestedCategory: String? = null,
)

@Dao
interface SmsImportDao {

    @androidx.room.Insert
    suspend fun insertBatch(batch: SmsImportBatchEntity): Long

    @androidx.room.Update
    suspend fun updateBatch(batch: SmsImportBatchEntity)

    @androidx.room.Query("SELECT * FROM sms_import_batches ORDER BY importedAt DESC LIMIT :limit")
    suspend fun latestBatches(limit: Int): List<SmsImportBatchEntity>

    @androidx.room.Query("SELECT * FROM sms_import_batches WHERE id = :id")
    suspend fun batchById(id: Long): SmsImportBatchEntity?

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: SmsImportEntity): Long

    @androidx.room.Query("SELECT * FROM sms_imports WHERE batchId = :batchId")
    suspend fun byBatch(batchId: Long): List<SmsImportEntity>

    @androidx.room.Query("SELECT * FROM sms_imports WHERE id = :id")
    suspend fun byId(id: Long): SmsImportEntity?

    @androidx.room.Query("SELECT COUNT(*) FROM sms_imports")
    suspend fun count(): Int

    @androidx.room.Query("SELECT EXISTS(SELECT 1 FROM sms_imports WHERE fingerprint = :fingerprint)")
    suspend fun hasFingerprint(fingerprint: String): Boolean

    @androidx.room.Query("SELECT fingerprint FROM sms_imports")
    suspend fun allFingerprints(): List<String>

    /**
     * Deletes one import row and its transaction. The transaction delete cascades to splits;
     * pre-existing transactions are untouched because each import row references only the
     * transaction its own batch created.
     */
    @androidx.room.Query(
        "DELETE FROM transactions WHERE id = (SELECT transactionId FROM sms_imports WHERE id = :importId)",
    )
    suspend fun deleteTransactionOf(importId: Long)

    @androidx.room.Query("DELETE FROM sms_imports WHERE id = :importId")
    suspend fun deleteRow(importId: Long)

    /** Undo: removes every transaction the batch created, then the rows and the batch itself. */
    @androidx.room.Query(
        "DELETE FROM transactions WHERE id IN (SELECT transactionId FROM sms_imports WHERE batchId = :batchId AND transactionId IS NOT NULL)",
    )
    suspend fun deleteTransactionsOf(batchId: Long)

    @androidx.room.Query("DELETE FROM sms_imports WHERE batchId = :batchId")
    suspend fun deleteRowsOf(batchId: Long)

    @androidx.room.Query("DELETE FROM sms_import_batches WHERE id = :batchId")
    suspend fun deleteBatch(batchId: Long)

    // ---- Permissionless capture: rows without a transaction await the user's confirmation ----

    /** The persistent pseudo-batch that holds shared/pasted candidates before they are confirmed. */
    @androidx.room.Query("SELECT * FROM sms_import_batches WHERE label = 'Pending review' LIMIT 1")
    suspend fun pendingBatch(): SmsImportBatchEntity?

    /** Candidates waiting for review: no transaction yet, newest first. */
    @androidx.room.Query("SELECT * FROM sms_imports WHERE transactionId IS NULL ORDER BY timestamp DESC")
    suspend fun pending(): List<SmsImportEntity>

    @androidx.room.Query("SELECT COUNT(*) FROM sms_imports WHERE transactionId IS NULL")
    suspend fun pendingCount(): Int

    /** Attaches the confirmed transaction to its import row and moves the row into the real batch. */
    @androidx.room.Query("UPDATE sms_imports SET transactionId = :transactionId, batchId = :batchId WHERE id = :importId")
    suspend fun linkTransaction(importId: Long, transactionId: Long, batchId: Long)

    /** Every SMS import row with its transaction, newest first — the management list. */
    @androidx.room.Query(
        "SELECT sms_imports.id AS importId, sms_imports.transactionId AS transactionId, " +
            "sms_imports.bank AS bank, sms_imports.sender AS sender, sms_imports.amount AS amount, " +
            "sms_imports.type AS type, sms_imports.merchant AS merchant, sms_imports.timestamp AS timestamp, " +
            "sms_imports.confidence AS confidence, sms_imports.batchId AS batchId, " +
            "transactions.reviewed AS reviewed, transactions.suspicious AS suspicious " +
            "FROM sms_imports LEFT JOIN transactions ON transactions.id = sms_imports.transactionId " +
            "ORDER BY sms_imports.timestamp DESC LIMIT :limit",
    )
    suspend fun managementRows(limit: Int): List<ManagementRow>

    /** Row of the management list: an import joined with what Review still needs from it. */
    data class ManagementRow(
        val importId: Long,
        val transactionId: Long?,
        val bank: String,
        val sender: String,
        val amount: Long,
        val type: TxnType,
        val merchant: String?,
        val timestamp: Long,
        val confidence: SmsConfidence,
        val batchId: Long,
        val reviewed: Boolean?,
        val suspicious: Boolean?,
    )
}
