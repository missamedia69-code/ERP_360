package com.missa.b360

import androidx.sqlite.db.SupportSQLiteDatabase
import com.missa.b360.core.data.db.AppDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurMigrationTest {
    private val sql = AppDatabase.FOURNISSEUR_STATEMENTS

    @Test fun `la migration 23 vers 24 execute exactement ses instructions`() {
        val executees = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") executees += args!![0] as String
            null
        } as SupportSQLiteDatabase
        assertEquals(23, AppDatabase.MIGRATION_23_24.startVersion)
        assertEquals(24, AppDatabase.MIGRATION_23_24.endVersion)
        AppDatabase.MIGRATION_23_24.migrate(db)
        assertEquals(sql, executees)
    }

    @Test fun `aucune instruction ne supprime ou ne reecrit une table existante`() {
        assertTrue(sql.none { it.contains("DROP", ignoreCase = true) || it.contains("DELETE", ignoreCase = true) })
        assertTrue(sql.none { it.contains("RENAME", ignoreCase = true) || it.contains("INSERT", ignoreCase = true) })
    }

    @Test fun `les nouvelles tables et index sont crees de facon idempotente`() {
        val creations = sql.filter { it.startsWith("CREATE") }
        assertEquals(5, creations.size)
        assertEquals(3, creations.count { it.startsWith("CREATE TABLE IF NOT EXISTS") })
        assertEquals(2, creations.count { it.startsWith("CREATE INDEX IF NOT EXISTS") })
        assertTrue(creations.filter { it.startsWith("CREATE TABLE") }.all { it.contains("REFERENCES `fournisseurs`(`id`)") })
    }

    @Test fun `les colonnes ajoutees aux tables existantes ont une valeur par defaut`() {
        val alters = sql.filter { it.startsWith("ALTER TABLE") }
        assertEquals(2, alters.size)
        assertTrue(alters.all { it.contains("ADD COLUMN") && it.contains("NOT NULL DEFAULT 0") })
        assertTrue(alters.any { it.contains("`fournisseur_documents`") && it.contains("`archive`") })
        assertTrue(alters.any { it.contains("`fournisseur_comptes_bancaires`") && it.contains("`modifieLe`") })
    }

    @Test fun `chaque colonne numerique non nulle des caches a une valeur par defaut`() {
        val balances = sql.first { it.contains("`fournisseur_balances` (") }
        listOf("dette", "enRetard", "joursRetardMax", "nbFacturesOuvertes", "nbCommandesOuvertes", "achats12Mois", "majAt").forEach {
            assertTrue(it, Regex("`$it` \\w+ NOT NULL DEFAULT 0").containsMatchIn(balances))
        }
        val plans = sql.first { it.contains("`fournisseur_paiements_planifies` (") }
        assertTrue(plans.contains("`statut` TEXT NOT NULL DEFAULT 'PLANIFIE'"))
    }
}
