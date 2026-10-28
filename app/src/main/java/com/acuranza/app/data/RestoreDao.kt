package com.acuranza.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Bulk reads and writes for backup and restore. Restore deletes and re-inserts everything inside a single
 * transaction (see `security.BackupManager`), so a failure or cancellation leaves the old data untouched.
 */
@Dao
interface RestoreDao {
    // Children before parents, so foreign keys are never violated mid-transaction.
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM card_bills") suspend fun clearBills()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM merchant_rules") suspend fun clearRules()
    @Query("DELETE FROM net_worth_history") suspend fun clearNetWorth()
    @Query("DELETE FROM deleted_fingerprints") suspend fun clearTombstones()
    @Query("DELETE FROM attachments") suspend fun clearAttachments()
    @Query("DELETE FROM txn_splits") suspend fun clearSplits()
    @Query("DELETE FROM recurring_rules") suspend fun clearRecurring()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM loans") suspend fun clearLoans()
    @Query("DELETE FROM split_settlements") suspend fun clearSplitSettlements()
    @Query("DELETE FROM split_shares") suspend fun clearSplitShares()
    @Query("DELETE FROM split_expenses") suspend fun clearSplitExpenses()
    @Query("DELETE FROM split_group_participants") suspend fun clearSplitGroupParticipants()
    @Query("DELETE FROM split_groups") suspend fun clearSplitGroups()
    @Query("DELETE FROM purchase_attachments") suspend fun clearPurchaseAttachments()
    @Query("DELETE FROM purchase_records") suspend fun clearPurchaseRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccounts(items: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransactions(items: List<TransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBills(items: List<CardBillEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudgets(items: List<BudgetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(items: List<GoalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRules(items: List<MerchantRuleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNetWorth(items: List<NetWorthSnapshotEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTombstones(items: List<DeletedFingerprintEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplits(items: List<SplitEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDebts(items: List<DebtEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRecurring(items: List<RecurringRuleEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLoans(items: List<LoanEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAttachments(items: List<AttachmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplitGroups(items: List<SplitGroupEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplitGroupParticipants(items: List<SplitGroupParticipantEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplitExpenses(items: List<SplitExpenseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplitShares(items: List<SplitShareEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSplitSettlements(items: List<SplitSettlementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPurchaseRecords(items: List<PurchaseRecordEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPurchaseAttachments(items: List<PurchaseAttachmentEntity>)

    @Query("SELECT * FROM accounts") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM card_bills") suspend fun bills(): List<CardBillEntity>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<BudgetEntity>
    @Query("SELECT * FROM goals") suspend fun goals(): List<GoalEntity>
    @Query("SELECT * FROM merchant_rules") suspend fun rules(): List<MerchantRuleEntity>
    @Query("SELECT * FROM net_worth_history") suspend fun netWorth(): List<NetWorthSnapshotEntity>
    @Query("SELECT * FROM deleted_fingerprints") suspend fun tombstones(): List<DeletedFingerprintEntity>
    @Query("SELECT * FROM txn_splits") suspend fun splits(): List<SplitEntity>
    @Query("SELECT * FROM debts") suspend fun debts(): List<DebtEntity>
    @Query("SELECT * FROM recurring_rules") suspend fun recurring(): List<RecurringRuleEntity>
    @Query("SELECT * FROM loans") suspend fun loans(): List<LoanEntity>
    @Query("SELECT * FROM attachments") suspend fun attachments(): List<AttachmentEntity>
    @Query("SELECT * FROM split_groups") suspend fun splitGroups(): List<SplitGroupEntity>
    @Query("SELECT * FROM split_group_participants") suspend fun splitGroupParticipants(): List<SplitGroupParticipantEntity>
    @Query("SELECT * FROM split_expenses") suspend fun splitExpenses(): List<SplitExpenseEntity>
    @Query("SELECT * FROM split_shares") suspend fun splitShares(): List<SplitShareEntity>
    @Query("SELECT * FROM split_settlements") suspend fun splitSettlements(): List<SplitSettlementEntity>
    @Query("SELECT * FROM purchase_records") suspend fun purchaseRecords(): List<PurchaseRecordEntity>
    @Query("SELECT * FROM purchase_attachments") suspend fun purchaseAttachments(): List<PurchaseAttachmentEntity>
}
