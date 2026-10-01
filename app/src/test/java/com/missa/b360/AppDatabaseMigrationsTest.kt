package com.missa.b360

import androidx.sqlite.db.SupportSQLiteDatabase
import com.missa.b360.core.data.db.AppDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDatabaseMigrationsTest {

    @Test fun `la chaine de migrations est contigue de la version 1 a la version 23`() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertEquals(22, migrations.size)
        migrations.forEachIndexed { index, migration ->
            assertEquals("début de la migration n°${index + 1}", index + 1, migration.startVersion)
            assertEquals("fin de la migration n°${index + 1}", index + 2, migration.endVersion)
        }
    }

    @Test fun `la migration 22 vers 23 execute exactement ses instructions de creation`() {
        val executees = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") executees += args!![0] as String
            null
        } as SupportSQLiteDatabase
        AppDatabase.MIGRATION_22_23.migrate(db)
        assertEquals(AppDatabase.CLIENT_ACCOUNT_STATEMENTS, executees)
    }

    @Test fun `la migration 22 vers 23 ne modifie que de nouvelles tables`() {
        val sql = AppDatabase.CLIENT_ACCOUNT_STATEMENTS
        assertEquals(10, sql.size)
        assertTrue(sql.all { it.startsWith("CREATE TABLE IF NOT EXISTS") || it.contains("INDEX IF NOT EXISTS") })
        assertTrue(sql.none { it.contains("DROP", ignoreCase = true) || it.contains("ALTER", ignoreCase = true) })
        val tables = sql.filter { it.startsWith("CREATE TABLE") }
        assertEquals(3, tables.size)
        assertTrue(tables.all { it.contains("REFERENCES `clients`(`id`)") })
    }

    @Test fun `chaque colonne numerique non nulle des comptes a une valeur par defaut`() {
        val balances = AppDatabase.CLIENT_ACCOUNT_STATEMENTS.first { it.contains("`client_balances` (") }
        listOf("encours", "enRetard", "joursRetardMax", "ca12Mois", "nbVentes", "majAt").forEach { colonne ->
            assertTrue(colonne, Regex("`$colonne` \\w+ NOT NULL DEFAULT 0").containsMatchIn(balances))
        }
        val suivi = AppDatabase.CLIENT_ACCOUNT_STATEMENTS.first { it.contains("`client_followups` (") }
        assertTrue(suivi.contains("`statut` TEXT NOT NULL DEFAULT 'OUVERT'"))
    }
}
