package com.acuranza.app.repo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.acuranza.app.data.PurchaseAttachmentEntity
import com.acuranza.app.data.PurchaseRecordEntity
import com.acuranza.app.data.SplitExpenseEntity
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.data.SplitGroupParticipantEntity
import com.acuranza.app.data.SplitShareEntity
import com.acuranza.app.data.SplitSettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitGroupDao {
    @Query("SELECT * FROM split_groups WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<SplitGroupEntity>>

    @Query("SELECT * FROM split_groups ORDER BY archived, name")
    fun observeAll(): Flow<List<SplitGroupEntity>>

    @Query("SELECT * FROM split_groups WHERE id = :id")
    suspend fun byId(id: Long): SplitGroupEntity?

    @Upsert
    suspend fun upsert(group: SplitGroupEntity): Long

    @Query("DELETE FROM split_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE split_groups SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface SplitGroupParticipantDao {
    @Query("SELECT * FROM split_group_participants WHERE groupId = :groupId ORDER BY name")
    fun observeForGroup(groupId: Long): Flow<List<SplitGroupParticipantEntity>>

    @Query("SELECT * FROM split_group_participants WHERE groupId = :groupId ORDER BY name")
    suspend fun forGroup(groupId: Long): List<SplitGroupParticipantEntity>

    @Query("SELECT * FROM split_group_participants WHERE id = :id")
    suspend fun byId(id: Long): SplitGroupParticipantEntity?

    @Upsert
    suspend fun upsert(participant: SplitGroupParticipantEntity): Long

    @Query("DELETE FROM split_group_participants WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SplitExpenseDao {
    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    fun observeForGroup(groupId: Long): Flow<List<SplitExpenseEntity>>

    @Query("SELECT * FROM split_expenses WHERE id = :id")
    suspend fun byId(id: Long): SplitExpenseEntity?

    @Upsert
    suspend fun upsert(expense: SplitExpenseEntity): Long

    @Query("DELETE FROM split_expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM split_shares WHERE expenseId = :expenseId")
    suspend fun clearShares(expenseId: Long)

    @Insert
    suspend fun insertShares(shares: List<SplitShareEntity>)

    @Query("SELECT * FROM split_shares WHERE expenseId = :expenseId ORDER BY id")
    suspend fun sharesFor(expenseId: Long): List<SplitShareEntity>

    @Query("SELECT * FROM split_shares WHERE expenseId IN (SELECT id FROM split_expenses WHERE groupId = :groupId) ORDER BY expenseId, id")
    fun observeAllSharesForGroup(groupId: Long): Flow<List<SplitShareEntity>>

    /** Replaces shares for an expense inside a transaction. */
    @Transaction
    suspend fun replaceShares(expenseId: Long, shares: List<SplitShareEntity>) {
        clearShares(expenseId)
        if (shares.isNotEmpty()) insertShares(shares.map { it.copy(id = 0, expenseId = expenseId) })
    }
}

@Dao
interface SplitSettlementDao {
    @Query("SELECT * FROM split_settlements WHERE groupId = :groupId AND reversed = 0 ORDER BY date DESC, id DESC")
    fun observeForGroup(groupId: Long): Flow<List<SplitSettlementEntity>>

    @Query("SELECT * FROM split_settlements WHERE groupId = :groupId ORDER BY date DESC, id DESC")
    suspend fun forGroup(groupId: Long): List<SplitSettlementEntity>

    @Upsert
    suspend fun upsert(settlement: SplitSettlementEntity): Long

    @Query("DELETE FROM split_settlements WHERE id = :id")
    suspend fun delete(id: Long)

    /** Reverses a settlement (kept for history, excluded from balances). */
    @Query("UPDATE split_settlements SET reversed = 1 WHERE id = :id")
    suspend fun reverse(id: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_records ORDER BY purchaseDate DESC, id DESC")
    fun observeAll(): Flow<List<PurchaseRecordEntity>>

    @Query("SELECT * FROM purchase_records WHERE id = :id")
    suspend fun byId(id: Long): PurchaseRecordEntity?

    @Query("""
        SELECT * FROM purchase_records
        WHERE (:query = '' OR lower(itemName) LIKE '%' || lower(:query) || '%'
             OR lower(merchant) LIKE '%' || lower(:query) || '%'
             OR lower(notes) LIKE '%' || lower(:query) || '%'
             OR lower(invoiceNo) LIKE '%' || lower(:query) || '%'
             OR lower(serialNo) LIKE '%' || lower(:query) || '%')
        AND (:category = '' OR category = :category)
        ORDER BY purchaseDate DESC, id DESC
    """)
    fun search(query: String, category: String): Flow<List<PurchaseRecordEntity>>

    @Upsert
    suspend fun upsert(record: PurchaseRecordEntity): Long

    @Query("DELETE FROM purchase_records WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PurchaseAttachmentDao {
    @Query("SELECT * FROM purchase_attachments WHERE purchaseId = :purchaseId ORDER BY createdAt")
    fun observeFor(purchaseId: Long): Flow<List<PurchaseAttachmentEntity>>

    @Query("SELECT * FROM purchase_attachments WHERE id = :id")
    suspend fun byId(id: Long): PurchaseAttachmentEntity?

    @Upsert
    suspend fun upsert(attachment: PurchaseAttachmentEntity): Long

    @Query("DELETE FROM purchase_attachments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM purchase_attachments WHERE purchaseId = :purchaseId")
    suspend fun deleteAllFor(purchaseId: Long)
}
