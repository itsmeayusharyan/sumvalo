package com.acuranza.app.repo

import com.acuranza.app.data.AcuranzaDatabase
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.model.TxnType
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import kotlinx.coroutines.flow.map

class CategoryRepository(private val db: AcuranzaDatabase) {

    fun observeAll(): Flow<List<CategoryEntity>> = db.categories().observeAll()

    fun observeByType(type: TxnType): Flow<List<CategoryEntity>> = observeAll().map { list -> list.filter { it.type == type } }

    fun observeByKey(): Flow<Map<String, CategoryEntity>> = observeAll().map { list -> list.associateBy { it.key } }

    suspend fun all(): List<CategoryEntity> = db.categories().all()

    suspend fun byKey(key: String): CategoryEntity? = all().firstOrNull { it.key == key }

    /** Adds a custom category; the key is derived from the name. Rejects duplicate names. */
    suspend fun addCustom(name: String, type: TxnType, icon: String, colorArgb: Long): String? {
        val trimmed = name.trim()
        CategoryRules.validateName(trimmed, all())?.let { return null }
        val base = trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "custom" }
        val existing = all().map { it.key }.toSet()
        var key = "custom_$base"
        var n = 2
        while (key in existing) key = "custom_${base}_${n++}"
        db.categories().upsert(CategoryEntity(key = key, name = trimmed, type = type, icon = icon, colorArgb = colorArgb, isDefault = false))
        return key
    }

    /** How many transactions still use this category. */
    suspend fun countTransactions(key: String): Int = db.categories().countTransactions(key)

    /**
     * Renames a category and/or restyles its icon and colour. Built-in categories may be restyled
     * (the SMS importer keeps matching by key); custom names must stay unique per type.
     */
    suspend fun update(entity: com.acuranza.app.data.CategoryEntity, name: String, icon: String, colorArgb: Long): String? {
        val trimmed = name.trim()
        CategoryRules.validateName(trimmed, all().filter { it.id != entity.id })?.let { return it }
        db.categories().upsert(entity.copy(name = trimmed, icon = icon, colorArgb = colorArgb))
        return null
    }

    /**
     * Deletes a custom category after moving everything that referenced it to [to]: transactions,
     * splits, recurring rules and learned merchant rules. A budget bound to the deleted category is
     * dropped (the replacement may already carry one; budgets are unique per category). Runs in
     * one database transaction so records are never left half-moved.
     */
    suspend fun deleteWithReassignment(key: String, to: String) {
        db.withTransaction {
            val dao = db.categories()
            dao.reassignTransactions(key, to)
            dao.reassignSplits(key, to)
            dao.reassignRecurring(key, to)
            dao.reassignMerchantRules(key, to)
            dao.deleteBudgetsFor(key)
            dao.deleteCustom(key)
        }
    }
}
