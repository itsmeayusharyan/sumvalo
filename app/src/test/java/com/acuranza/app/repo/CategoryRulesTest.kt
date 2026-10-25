package com.acuranza.app.repo

import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category editing rules are pure, so duplicate prevention, name validation, deletion protection
 * and reassignment choice are unit-tested directly against [CategoryRules].
 */
class CategoryRulesTest {

    private fun expense(key: String, name: String, isDefault: Boolean = true) =
        CategoryEntity(key = key, name = name, type = TxnType.EXPENSE, icon = "category", colorArgb = 0xFFADB5BD, isDefault = isDefault)

    private fun income(key: String, name: String, isDefault: Boolean = true) =
        CategoryEntity(key = key, name = name, type = TxnType.INCOME, icon = "payments", colorArgb = 0xFF2F9E44, isDefault = isDefault)

    private val existing = listOf(
        expense("food", "Food & Dining"),
        expense("other", "Other"),
        expense("custom_goa", "Goa trip", isDefault = false),
        income("salary", "Salary"),
        income("other_income", "Money received"),
    )

    // ---------------------------------------------------------------- names

    @Test
    fun `names must be non-blank and bounded`() {
        assertEquals("Give the category a name", CategoryRules.validateName("   ", existing))
        assertEquals("Give the category a name", CategoryRules.validateName("", existing))
        assertEquals("Keep the name under 24 characters", CategoryRules.validateName("x".repeat(25), existing))
        assertNull(CategoryRules.validateName("A brand new category", existing))
    }

    @Test
    fun `duplicate names are rejected case-insensitively`() {
        assertEquals(
            "A category with this name already exists",
            CategoryRules.validateName("food & dining", existing),
        )
        // An edit that keeps its own name passes; the caller excludes the edited row before calling.
        assertNull(CategoryRules.validateName("Food & Dining", existing.filter { it.key != "food" }))
    }

    @Test
    fun `same name across types is allowed`() {
        // "Salary" exists as income; an expense category of that name is a different row.
        assertNull(CategoryRules.validateName("Salary", existing.filter { it.type == TxnType.EXPENSE }))
    }

    // ---------------------------------------------------------------- deletion protection

    @Test
    fun `built-in categories are protected, custom ones are deletable`() {
        assertFalse(CategoryRules.isDeletable(existing.first { it.key == "food" }))
        assertTrue(CategoryRules.isDeletable(existing.first { it.key == "custom_goa" }))
    }

    // ---------------------------------------------------------------- reassignment

    @Test
    fun `reassignment offers same-type categories only, never itself`() {
        val deleting = existing.first { it.key == "custom_goa" }
        val options = CategoryRules.reassignmentOptions(existing, deleting)
        assertTrue(options.all { it.type == TxnType.EXPENSE })
        assertTrue(options.none { it.key == "custom_goa" })
        // The catch-all is the suggested replacement, not just the first row.
        assertEquals("other", CategoryRules.suggestedReplacement(existing, deleting)?.key)
    }

    @Test
    fun `income deletions fall back to the income catch-all`() {
        val deleting = income("custom_bonus", "Festival bonus", isDefault = false)
        val all = existing + deleting
        assertEquals("other_income", CategoryRules.suggestedReplacement(all, deleting)?.key)
    }

    @Test
    fun `with no catch-all present the first same-type option is suggested`() {
        val deleting = expense("custom_goa", "Goa trip", isDefault = false)
        val thin = listOf(expense("food", "Food & Dining"), deleting)
        assertEquals("food", CategoryRules.suggestedReplacement(thin, deleting)?.key)
    }
}
