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

/** Compare la migration 23 → 24 écrite à la main au schéma que Room exporte (24.json). */
class FournisseurSchemaExportTest {
    private val nouvellesTables = setOf("fournisseur_balances", "fournisseur_scores", "fournisseur_paiements_planifies")

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

    private fun colonnes(entite: JsonObject): Map<String, JsonObject> =
        entite.getValue("fields").jsonArray.map { it.jsonObject }.associateBy { it.getValue("columnName").jsonPrimitive.content }

    @Test fun `la version exportee est 24`() {
        assertEquals(24, schema(24).getValue("database").jsonObject.getValue("version").jsonPrimitive.content.toInt())
    }

    @Test fun `la migration cree exactement les tables et index du schema 24`() {
        val attendu = nouvellesTables.flatMap { sqlExporte(entites(schema(24)).getValue(it)) }
        val creations = AppDatabase.FOURNISSEUR_STATEMENTS.filter { it.startsWith("CREATE") }
        assertEquals(attendu.sorted(), creations.sorted())
    }

    @Test fun `les colonnes ajoutees correspondent aux champs exportes avec leur valeur par defaut`() {
        val apres = entites(schema(24))
        listOf("fournisseur_documents" to "archive", "fournisseur_comptes_bancaires" to "modifieLe").forEach { (table, colonne) ->
            val champ = colonnes(apres.getValue(table)).getValue(colonne)
            assertEquals("$table.$colonne NOT NULL", "true", champ.getValue("notNull").jsonPrimitive.content)
            assertEquals("$table.$colonne défaut", "0", champ.getValue("defaultValue").jsonPrimitive.content)
            assertTrue(AppDatabase.FOURNISSEUR_STATEMENTS.any {
                it == "ALTER TABLE `$table` ADD COLUMN `$colonne` INTEGER NOT NULL DEFAULT 0"
            })
        }
    }

    @Test fun `les autres tables sont identiques entre les schemas 23 et 24`() {
        val avant = entites(schema(23))
        val apres = entites(schema(24))
        val modifiees = setOf("fournisseur_documents", "fournisseur_comptes_bancaires")
        assertEquals(avant.keys, apres.keys - nouvellesTables)
        avant.filterKeys { it !in modifiees }.forEach { (table, definition) ->
            assertEquals("table $table modifiée", definition as JsonElement, apres.getValue(table) as JsonElement)
        }
        modifiees.forEach { table ->
            val ancien = colonnes(avant.getValue(table))
            val nouveau = colonnes(apres.getValue(table))
            assertEquals("colonnes existantes de $table", ancien, nouveau.filterKeys { it in ancien.keys })
            assertEquals("une seule colonne ajoutée à $table", 1, nouveau.size - ancien.size)
        }
    }
}
