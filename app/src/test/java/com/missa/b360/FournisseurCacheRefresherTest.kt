package com.missa.b360

import com.missa.b360.core.data.dao.FournisseurBalanceDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurScoreDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.FournisseurAchatMetrics
import com.missa.b360.core.domain.model.FournisseurCacheRules
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.usecase.FournisseurBalanceRefresher
import com.missa.b360.core.domain.usecase.FournisseurScoreRefresher
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Les refreshers n'ajoutent que de la persistance : le cache égale toujours le calculateur pur. */
class FournisseurCacheRefresherTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private class Memoire {
        val balances = linkedMapOf<Long, FournisseurBalanceEntity>()
        val scores = linkedMapOf<Long, FournisseurScoreEntity>()
    }

    private inline fun <reified T> faux(crossinline reponse: (String, Array<Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, m, args ->
            reponse(m.name, args ?: emptyArray())
        } as T

    private fun fournisseur(id: Long, jours: Int = 30) = FournisseurEntity(
        id = id, code = "F$id", nom = "Fournisseur $id", telephone = "600", createdAt = 0L, joursEcheance = jours,
    )

    private fun facture(id: Long, fournisseur: Long, total: Double, paye: Double, jourCreation: Long, statut: String = OperationStatus.VALIDATED.name) =
        OperationRecordEntity(
            id = id, module = OperationModule.ACHATS.name, reference = "FFR-$id", title = "FFR-$id", status = statut,
            notes = PurchaseRecordCodec.encode(
                PurchaseRecordPayload(
                    supplierId = fournisseur, supplierName = "F$fournisseur",
                    lines = listOf(PurchaseLine(id = 1, name = "x", unitPrice = total, quantity = 1.0)),
                    subtotal = total, taxRate = 0.0, taxAmount = 0.0, total = total,
                    paymentMethod = "Virement", paidAmount = paye,
                ),
            ),
            createdAt = jourCreation * jour,
        )

    private fun commande(id: Long, fournisseur: Long, jourCreation: Long) = OperationRecordEntity(
        id = id, module = OperationModule.ACHATS.name, reference = "BC-$id", title = "BC-$id",
        status = OperationStatus.VALIDATED.name,
        notes = CommandeAchatCodec.encode(
            CommandeAchatPayload(
                supplierId = fournisseur, supplierName = "F$fournisseur",
                lines = listOf(CommandeAchatLigne(id = 1, name = "m", quantity = 5.0, unitPrice = 10.0, productId = 9)),
            ),
        ),
        createdAt = jourCreation * jour,
    )

    private val pieces = listOf(
        facture(1, 1, 100.0, 40.0, 900),
        facture(2, 1, 200.0, 0.0, 960),
        facture(3, 2, 50.0, 0.0, 990),
        facture(4, 2, 70.0, 0.0, 970, OperationStatus.DRAFT.name),
        commande(5, 1, 980),
        commande(6, 2, 985),
    )
    private val fournisseurs = listOf(fournisseur(1), fournisseur(2), fournisseur(3))

    private fun operationDao() = faux<OperationRecordDao> { nom, _ ->
        if (nom == "getByModule") pieces else error("appel inattendu $nom")
    }

    private fun fournisseurDao() = faux<FournisseurDao> { nom, args ->
        when (nom) {
            "getAll" -> fournisseurs
            "getById" -> fournisseurs.firstOrNull { it.id == args[0] as Long }
            else -> error("appel inattendu $nom")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun balanceDao(m: Memoire) = faux<FournisseurBalanceDao> { nom, args ->
        when (nom) {
            "upsert" -> (args[0] as FournisseurBalanceEntity).let { m.balances[it.fournisseurId] = it }
            "upsertAll" -> (args[0] as List<FournisseurBalanceEntity>).forEach { m.balances[it.fournisseurId] = it }
            "getAll" -> m.balances.values.toList()
            "get" -> m.balances[args[0] as Long]
            "count" -> m.balances.size
            else -> error("appel inattendu $nom")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun scoreDao(m: Memoire) = faux<FournisseurScoreDao> { nom, args ->
        when (nom) {
            "upsert" -> (args[0] as FournisseurScoreEntity).let { m.scores[it.fournisseurId] = it }
            "upsertAll" -> (args[0] as List<FournisseurScoreEntity>).forEach { m.scores[it.fournisseurId] = it }
            "get" -> m.scores[args[0] as Long]
            "count" -> m.scores.size
            else -> error("appel inattendu $nom")
        }
    }

    @Test
    fun `le cache d un fournisseur egale le calculateur pur`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurBalanceRefresher(operationDao(), fournisseurDao(), balanceDao(m))
        val ecrit = refresher.rafraichir(1, now)
        val attendu = FournisseurCacheRules.balance(fournisseurs[0], pieces, now)
        assertEquals(attendu, ecrit)
        assertEquals(attendu, m.balances[1L])
        assertEquals(260.0, attendu.dette, 0.001)
        assertEquals(2, attendu.nbFacturesOuvertes)
        assertEquals(FournisseurAchatMetrics.commandesOuvertes(pieces, 1L), attendu.nbCommandesOuvertes)
    }

    @Test
    fun `un fournisseur inconnu ne cree aucune ligne`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurBalanceRefresher(operationDao(), fournisseurDao(), balanceDao(m))
        assertNull(refresher.rafraichir(99, now))
        assertNull(refresher.rafraichir(0, now))
        assertTrue(m.balances.isEmpty())
    }

    @Test
    fun `la reconstruction complete cree une ligne par fournisseur et la somme des dettes est la dette globale`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurBalanceRefresher(operationDao(), fournisseurDao(), balanceDao(m))
        assertEquals(3, refresher.reconstruireTout(now))
        assertEquals(setOf(1L, 2L, 3L), m.balances.keys)
        assertEquals(0.0, m.balances.getValue(3L).dette, 0.0)
        assertEquals(
            FournisseurAchatMetrics.dette(pieces, now, joursEcheance = { 30 }).dette,
            m.balances.values.sumOf { it.dette }, 0.001,
        )
        assertEquals(
            FournisseurAchatMetrics.commandesOuvertes(pieces),
            m.balances.values.sumOf { it.nbCommandesOuvertes },
        )
    }

    @Test
    fun `la verification de coherence corrige un solde faux et ne touche pas un solde juste`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurBalanceRefresher(operationDao(), fournisseurDao(), balanceDao(m))
        refresher.reconstruireTout(now)
        assertTrue(refresher.verifierCoherence(now + 3_600_000L).isEmpty())

        val juste = m.balances.getValue(1L)
        m.balances[1L] = juste.copy(dette = juste.dette + 40.0)
        m.balances.remove(2L)
        val corrigees = refresher.verifierCoherence(now)
        assertEquals(setOf(1L, 2L), corrigees.map { it.first }.toSet())
        assertEquals(juste.dette + 40.0, corrigees.first { it.first == 1L }.second!!.dette, 0.001)
        assertNull(corrigees.first { it.first == 2L }.second)
        assertEquals(juste.copy(majAt = now), m.balances[1L])
        assertNotNull(m.balances[2L])
    }

    @Test
    fun `une facture reglee ou annulee sort de la dette au prochain rafraichissement`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurBalanceRefresher(operationDao(), fournisseurDao(), balanceDao(m))
        val avant = FournisseurCacheRules.balance(fournisseurs[1], pieces, now)
        val apres = FournisseurCacheRules.balance(
            fournisseurs[1], pieces.map { if (it.id == 3L) it.copy(status = OperationStatus.CANCELLED.name) else it }, now,
        )
        assertEquals(50.0, avant.dette, 0.001)
        assertEquals(0.0, apres.dette, 0.0)
        assertEquals(0, apres.nbFacturesOuvertes)
        refresher.rafraichir(2, now)
        assertEquals(avant, m.balances[2L])
    }

    @Test
    fun `le score est ecrit sans valeur pour un fournisseur sans mesure`() = runBlocking {
        val m = Memoire()
        val refresher = FournisseurScoreRefresher(operationDao(), fournisseurDao(), scoreDao(m))
        val score = refresher.rafraichir(1, now)!!
        assertEquals(0, score.nbCommandesMesurees)
        assertNull(score.score)
        assertEquals(3, refresher.reconstruireTout(now))
        assertEquals(setOf(1L, 2L, 3L), m.scores.keys)
        assertNull(refresher.rafraichir(42, now))
    }

    @Test
    fun `les commandes ouvertes se filtrent par fournisseur`() {
        assertEquals(1, FournisseurAchatMetrics.commandesOuvertes(pieces, 1L))
        assertEquals(1, FournisseurAchatMetrics.commandesOuvertes(pieces, 2L))
        assertEquals(0, FournisseurAchatMetrics.commandesOuvertes(pieces, 3L))
        assertEquals(2, FournisseurAchatMetrics.commandesOuvertes(pieces))
    }
}
