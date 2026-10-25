package com.acuranza.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the exported Room schemas against [AcuranzaDatabase.MIGRATION_1_2]: v2 must be v1 plus exactly the
 * `transactions.suspicious` column the migration adds, declared NOT NULL DEFAULT 0 so Room's validation
 * accepts migrated databases. (A full MigrationTestHelper run needs a device.)
 */
class MigrationSchemaTest {

    private fun schema(version: Int): JSONObject {
        val dir = listOf("schemas", "app/schemas").map { File(it, "com.acuranza.app.data.AcuranzaDatabase") }.first { it.exists() }
        return JSONObject(File(dir, "$version.json").readText()).getJSONObject("database")
    }

    private fun columns(db: JSONObject, table: String): Map<String, JSONObject> {
        val entities = db.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            if (e.getString("tableName") == table) {
                val fields = e.getJSONArray("fields")
                return (0 until fields.length()).associate { fields.getJSONObject(it).let { f -> f.getString("columnName") to f } }
            }
        }
        error("no table $table")
    }

    @Test
    fun version2AddsOnlyTheSuspiciousColumn() {
        val v1 = columns(schema(1), "transactions")
        val v2 = columns(schema(2), "transactions")
        assertEquals(setOf("suspicious"), v2.keys - v1.keys)
        assertEquals(v1.keys, v1.keys intersect v2.keys)
        val col = v2.getValue("suspicious")
        assertEquals("INTEGER", col.getString("affinity"))
        assertTrue(col.getBoolean("notNull"))
        assertEquals("0", col.getString("defaultValue"))
    }

    @Test
    fun otherTablesAreUnchanged() {
        listOf("accounts", "categories", "card_bills", "budgets", "goals", "investments").forEach { table ->
            assertEquals(table, columns(schema(1), table).keys, columns(schema(2), table).keys)
        }
    }

    @Test
    fun version4AddsSavingsInterestColumnsToAccounts() {
        val v3 = columns(schema(3), "accounts")
        val v4 = columns(schema(4), "accounts")
        assertEquals(setOf("interestRatePct", "interestCreditFrequency"), v4.keys - v3.keys)
        val rate = v4.getValue("interestRatePct")
        assertEquals("REAL", rate.getString("affinity"))
        assertTrue(!rate.getBoolean("notNull"))
        val freq = v4.getValue("interestCreditFrequency")
        assertEquals("TEXT", freq.getString("affinity"))
        assertTrue(freq.getBoolean("notNull"))
        assertEquals("'QUARTERLY'", freq.getString("defaultValue"))
    }

    private fun tables(db: JSONObject): Set<String> {
        val entities = db.getJSONArray("entities")
        return (0 until entities.length()).map { entities.getJSONObject(it).getString("tableName") }.toSet()
    }

    @Test
    fun version5AddsOnlyTheTombstoneAndDemoTables() {
        assertEquals(setOf("deleted_fingerprints", "demo_rows"), tables(schema(5)) - tables(schema(4)))
        tables(schema(4)).forEach { table ->
            assertEquals(table, columns(schema(4), table).keys, columns(schema(5), table).keys)
        }
        val tomb = columns(schema(5), "deleted_fingerprints")
        assertEquals(setOf("fingerprint", "deletedAt"), tomb.keys)
        assertTrue(tomb.values.all { it.getBoolean("notNull") })
        assertEquals(setOf("kind", "rowKey"), columns(schema(5), "demo_rows").keys)
    }

    @Test
    fun migration4To5CreatesTheSameTablesRoomExpects() {
        val sql = mutableListOf<String>()
        val db = java.lang.reflect.Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java),
        ) { _, m, args -> if (m.name == "execSQL") sql += args[0] as String; null } as androidx.sqlite.db.SupportSQLiteDatabase
        AcuranzaDatabase.MIGRATION_4_5.migrate(db)
        val entities = schema(5).getJSONArray("entities")
        val expected = (0 until entities.length()).map { entities.getJSONObject(it) }
            .filter { it.getString("tableName") in setOf("deleted_fingerprints", "demo_rows") }
            .map { it.getString("createSql").replace("\${TABLE_NAME}", it.getString("tableName")) }
        fun norm(s: String) = s.replace("IF NOT EXISTS", "").replace(Regex("\\s+"), " ").trim()
        assertEquals(expected.map(::norm).toSet(), sql.map(::norm).toSet())
    }

    @Test
    fun version7AddsTagsRolloverAndTheNewTables() {
        val newTables = setOf("txn_splits", "debts", "recurring_rules", "loans", "attachments")
        assertEquals(newTables, tables(schema(7)) - tables(schema(6)))
        tables(schema(6)).forEach { table ->
            val added = columns(schema(7), table).keys - columns(schema(6), table).keys
            val expected = mapOf("transactions" to setOf("tags"), "budgets" to setOf("rollover"))[table].orEmpty()
            assertEquals(table, expected, added)
            assertTrue(table, columns(schema(6), table).keys.all { it in columns(schema(7), table) })
        }
        val tags = columns(schema(7), "transactions").getValue("tags")
        assertTrue(tags.getBoolean("notNull"))
        assertEquals("''", tags.getString("defaultValue"))
        val rollover = columns(schema(7), "budgets").getValue("rollover")
        assertTrue(rollover.getBoolean("notNull"))
        assertEquals("0", rollover.getString("defaultValue"))
    }

    @Test
    fun migration6To7RunsExactlyWhatRoomExpects() {
        val sql = mutableListOf<String>()
        val db = java.lang.reflect.Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java),
        ) { _, m, args -> if (m.name == "execSQL") sql += args[0] as String; null } as androidx.sqlite.db.SupportSQLiteDatabase
        AcuranzaDatabase.MIGRATION_6_7.migrate(db)
        val newTables = setOf("txn_splits", "debts", "recurring_rules", "loans", "attachments")
        val entities = schema(7).getJSONArray("entities")
        val expected = mutableListOf(
            "ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE budgets ADD COLUMN rollover INTEGER NOT NULL DEFAULT 0",
        )
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val name = e.getString("tableName")
            if (name !in newTables) continue
            expected += e.getString("createSql").replace("\${TABLE_NAME}", name)
            val indices = e.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) expected += indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name)
        }
        fun norm(s: String) = s.replace("IF NOT EXISTS", "").replace(Regex("\\s+"), " ").trim()
        assertEquals(expected.map(::norm).toSet(), sql.map(::norm).toSet())
        assertEquals(expected.size, sql.size)
    }

    @Test
    fun version8AddsOnlyTheOtherDigitsColumnToAccounts() {
        assertEquals(tables(schema(7)), tables(schema(8)))
        val v7 = columns(schema(7), "accounts")
        val v8 = columns(schema(8), "accounts")
        assertEquals(setOf("otherDigits"), v8.keys - v7.keys)
        val col = v8.getValue("otherDigits")
        assertEquals("TEXT", col.getString("affinity"))
        assertTrue(!col.getBoolean("notNull"))
        tables(schema(7)).filter { it != "accounts" }.forEach { table ->
            assertEquals(table, columns(schema(7), table).keys, columns(schema(8), table).keys)
        }
    }

    private fun indexSql(db: JSONObject, table: String): Set<String> {
        val entities = db.getJSONArray("entities")
        val e = (0 until entities.length()).map { entities.getJSONObject(it) }.first { it.getString("tableName") == table }
        val indices = e.optJSONArray("indices") ?: return emptySet()
        return (0 until indices.length()).map { indices.getJSONObject(it).getString("createSql").replace("\${TABLE_NAME}", table) }.toSet()
    }

    @Test
    fun migration9To10RemovesTheRemovedFeatureTables() {
        val sql = mutableListOf<String>()
        val db = java.lang.reflect.Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java),
        ) { _, m, args -> if (m.name == "execSQL") sql += args[0] as String; null } as androidx.sqlite.db.SupportSQLiteDatabase
        AcuranzaDatabase.MIGRATION_9_10.migrate(db)
        assertEquals(
            setOf(
                "DROP TABLE IF EXISTS `investments`",
                "DROP TABLE IF EXISTS `investment_history`",
                "DROP TABLE IF EXISTS `household_items`",
                "DROP TABLE IF EXISTS `net_worth_history`",
                "CREATE TABLE IF NOT EXISTS `net_worth_history` (`epochDay` INTEGER NOT NULL, `liquid` INTEGER NOT NULL, " +
                    "`cardDebt` INTEGER NOT NULL, `total` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))",
            ),
            sql.toSet(),
        )
    }

    @Test
    fun version10DropsInvestmentsHouseholdAndTheNetWorthColumn() {
        val v10 = schema(10)
        val tables = tables(v10)
        assertTrue("investments" !in tables)
        assertTrue("investment_history" !in tables)
        assertTrue("household_items" !in tables)
        assertEquals(setOf("epochDay", "liquid", "cardDebt", "total"), columns(v10, "net_worth_history").keys)
    }

    @Test
    fun version13AddsOnlyTheSuggestedCategoryColumn() {
        val v12 = schema(12)
        val v13 = schema(13)
        // v13 adds exactly one nullable column to sms_imports; nothing else moves.
        assertEquals(tables(v13), tables(v12))
        assertEquals(setOf("suggestedCategory"), columns(v13, "sms_imports").keys - columns(v12, "sms_imports").keys)
        tables(v12).forEach { table ->
            if (table == "sms_imports") return@forEach // the one table v13 changes
            assertEquals(table, columns(v12, table).keys, columns(v13, table).keys)
            assertEquals(table, indexSql(v12, table), indexSql(v13, table))
        }
        assertFalse(columns(v13, "sms_imports").getValue("suggestedCategory").getBoolean("notNull"))
    }

    @Test
    fun version12AddsOnlyTheSmsImportTables() {
        val v11 = schema(11)
        val v12 = schema(12)
        // v12 adds exactly the two SMS bookkeeping tables; nothing existing changes shape.
        assertEquals(tables(v12) - tables(v11), setOf("sms_import_batches", "sms_imports"))
        tables(v11).forEach { table ->
            assertEquals(table, columns(v11, table).keys, columns(v12, table).keys)
            assertEquals(table, indexSql(v11, table), indexSql(v12, table))
        }
        // The import rows carry no message body: only the extracted fields plus a fingerprint.
        assertEquals(
            setOf(
                "id", "batchId", "transactionId", "fingerprint", "sender", "bank", "accountLast4",
                "type", "amount", "merchant", "timestamp", "channel", "confidence",
            ),
            columns(v12, "sms_imports").keys,
        )
        assertEquals(setOf("id", "importedAt", "txnCount", "label", "undone"), columns(v12, "sms_import_batches").keys)
    }

    @Test
    fun version9AddsOnlyTheTwoTransactionIndexes() {
        assertEquals(tables(schema(8)), tables(schema(9)))
        tables(schema(8)).forEach { table ->
            assertEquals(table, columns(schema(8), table).keys, columns(schema(9), table).keys)
            if (table != "transactions") assertEquals(table, indexSql(schema(8), table), indexSql(schema(9), table))
        }
        assertEquals(
            setOf(
                "CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)",
                "CREATE INDEX IF NOT EXISTS `index_transactions_referenceNo` ON `transactions` (`referenceNo`)",
            ),
            indexSql(schema(9), "transactions") - indexSql(schema(8), "transactions"),
        )
    }
}
