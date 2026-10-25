package com.acuranza.app.repo

import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.model.TxnType

/**
 * The pure rules for editing categories, kept free of DataStore and DB so they are unit-testable:
 * name validation, which categories may be deleted, and where a deleted category's records go.
 */
object CategoryRules {
    const val MAX_NAME = 24

    /** Built-in fallbacks when a deleted category needs a safe home for its records. */
    const val FALLBACK_EXPENSE = "other"
    const val FALLBACK_INCOME = "other_income"

    /**
     * Returns null when [raw] is a valid category name, or the message to show. Names must be
     * non-blank and unique per type, case-insensitively — "Food" and "food" would be the same row.
     */
    fun validateName(raw: String, existing: List<CategoryEntity>): String? {
        val name = raw.trim()
        if (name.isEmpty()) return "Give the category a name"
        if (name.length > MAX_NAME) return "Keep the name under $MAX_NAME characters"
        return if (existing.any { it.name.equals(name, ignoreCase = true) }) {
            "A category with this name already exists"
        } else {
            null
        }
    }

    /** Only custom categories can be deleted; built-in keys are load-bearing for imports and stats. */
    fun isDeletable(category: CategoryEntity): Boolean = !category.isDefault

    /** The categories a deleted one's records can move to: same type, excluding itself. */
    fun reassignmentOptions(all: List<CategoryEntity>, deleting: CategoryEntity): List<CategoryEntity> =
        all.filter { it.type == deleting.type && it.key != deleting.key }

    /** The preselected replacement: the type's catch-all, else the first option. */
    fun suggestedReplacement(all: List<CategoryEntity>, deleting: CategoryEntity): CategoryEntity? {
        val options = reassignmentOptions(all, deleting)
        val fallbackKey = if (deleting.type == TxnType.EXPENSE) FALLBACK_EXPENSE else FALLBACK_INCOME
        return options.firstOrNull { it.key == fallbackKey } ?: options.firstOrNull()
    }
}
