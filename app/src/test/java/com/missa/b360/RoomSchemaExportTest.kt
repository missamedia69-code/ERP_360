package com.missa.b360

import com.missa.b360.core.data.db.AppDatabase
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Compare la migration 22 → 23 écrite à la main au schéma que Room exporte (23.json). */
class RoomSchemaExportTest {
    private val nouvellesTables = setOf("client_balances", "client_followups", "client_payments")

    private fun schema(version: Int): JsonObject {
        val dossier = listOf("schemas", "app/schemas")
            .map { File(it, "com.missa.b360.core.data.db.AppDatabase") }
            .firstOrNull { it.isDirectory }
            ?: error("dossier de schémas Room introuvable")
        val fichier = File(dossier, "$version.json")
        assertTrue("schéma $version absent", fichier.isFile)
        return Json.parseToJsonElement(fichier.readText()).jsonObject
    }

    private fun entites(schema: JsonObject): Map<String, JsonObject> =
        schema.getValue("database").jsonObject.getValue("entities").jsonArray
            .map { it.jsonObject }
            .associateBy { it.getValue("tableName").jsonPrimitive.content }

    private fun sqlExporte(entite: JsonObject): List<String> {
        val table = entite.getValue("tableName").jsonPrimitive.content
        val indices = (entite["indices"] as? JsonArray).orEmpty().map { it.jsonObject }
        return (listOf(entite.getValue("createSql").jsonPrimitive.content) +
            indices.map { it.getValue("createSql").jsonPrimitive.content })
            .map { it.replace("\${TABLE_NAME}", table) }
    }

    @Test fun `la version exportee est 23`() {
        assertEquals(23, schema(23).getValue("database").jsonObject.getValue("version").jsonPrimitive.content.toInt())
    }

    @Test fun `la migration cree exactement les tables et index du schema 23`() {
        val attendu = nouvellesTables.flatMap { sqlExporte(entites(schema(23)).getValue(it)) }
        assertEquals(attendu.sorted(), AppDatabase.CLIENT_ACCOUNT_STATEMENTS.sorted())
    }

    @Test fun `les tables existantes sont identiques entre les schemas 22 et 23`() {
        val avant = entites(schema(22))
        val apres = entites(schema(23))
        assertEquals(avant.keys, apres.keys - nouvellesTables)
        avant.forEach { (table, definition) ->
            assertEquals("table $table modifiée", definition as JsonElement, apres.getValue(table) as JsonElement)
        }
    }
}
