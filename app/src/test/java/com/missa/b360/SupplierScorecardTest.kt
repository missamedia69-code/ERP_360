package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import com.missa.b360.core.domain.model.SupplierScoreStatut
import com.missa.b360.core.domain.model.SupplierScorecard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class SupplierScorecardTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour
    private val fournisseur = 7L
    private val zone = ZoneOffset.UTC

    private var prochainId = 10_000L

    private fun piece(notes: String, jourCreation: Long, id: Long = prochainId++, statut: String = OperationStatus.VALIDATED.name) =
        OperationRecordEntity(
            id = id, module = OperationModule.ACHATS.name, reference = "P-$id", title = "P-$id",
            status = statut, notes = notes, createdAt = jourCreation * jour,
        )

    private fun commande(
        id: Long, jourCreation: Long, quantite: Double = 10.0, prix: Double = 100.0,
        prevueJour: Long? = null, fournisseurId: Long = fournisseur, produit: Long? = 55,
    ) = piece(
        CommandeAchatCodec.encode(
            CommandeAchatPayload(
                supplierId = fournisseurId, supplierName = "F",
                lines = listOf(CommandeAchatLigne(id = 1, name = "Matière", quantity = quantite, unitPrice = prix, productId = produit)),
                dateLivraisonPrevue = prevueJour?.let { it * jour + 3_600_000L },
            ),
        ),
        jourCreation, id,
    )

    private fun reception(
        commandeId: Long, jourCreation: Long, quantite: Double, prixReel: Double? = null, produit: Long = 55,
    ) = piece(
        ReceptionCodec.encode(
            ReceptionPayload(
                commandeRecordId = commandeId, supplierId = fournisseur, supplierName = "F",
                lignes = listOf(ReceptionLigne(productId = produit, name = "Matière", quantiteRecue = quantite, prixReel = prixReel)),
            ),
        ),
        jourCreation,
    )

    private fun calcul(pieces: List<OperationRecordEntity>) = SupplierScorecard.calculer(fournisseur, pieces, now, zone)

    /** n commandes parfaites : reçues le jour prévu, quantité exacte. */
    private fun parfaites(n: Int): List<OperationRecordEntity> = (1..n).flatMap { i ->
        val id = 100L + i
        listOf(commande(id, 700L + i * 10, prevueJour = 710L + i * 10), reception(id, 710L + i * 10, 10.0))
    }

    @Test
    fun `aucune commande mesuree donne un score non significatif`() {
        val s = calcul(emptyList())
        assertEquals(SupplierScoreStatut.NON_SIGNIFICATIF, s.statut)
        assertNull(s.score)
        assertEquals(0, s.nbCommandesMesurees)
    }

    @Test
    fun `deux commandes mesurees ne suffisent pas`() {
        val s = calcul(parfaites(2))
        assertEquals(2, s.nbCommandesMesurees)
        assertNull(s.score)
        assertEquals(SupplierScoreStatut.NON_SIGNIFICATIF, s.statut)
        assertEquals(100.0, s.ponctualite ?: -1.0, 0.001)
    }

    @Test
    fun `trois commandes parfaites donnent cent`() {
        val s = calcul(parfaites(3))
        assertEquals(SupplierScoreStatut.SIGNIFICATIF, s.statut)
        assertEquals(100, s.score)
        assertEquals(100.0, s.conformite ?: -1.0, 0.001)
        assertNull(s.prix)
        assertEquals(10.0, s.delaiMoyenReelJours ?: -1.0, 0.001)
    }

    @Test
    fun `une commande partiellement recue n est pas mesuree`() {
        val pieces = parfaites(3) + commande(900, 760) + reception(900, 765, 4.0)
        assertEquals(3, calcul(pieces).nbCommandesMesurees)
    }

    @Test
    fun `une commande hors fenetre de 365 jours ou d un autre fournisseur est ignoree`() {
        val ancienne = listOf(commande(800, 100), reception(800, 110, 10.0))
        val autre = listOf(commande(801, 700, fournisseurId = 99), reception(801, 710, 10.0))
        assertEquals(3, calcul(parfaites(3) + ancienne + autre).nbCommandesMesurees)
    }

    @Test
    fun `ponctualite fin de journee prevue incluse puis retard`() {
        // prévue jour 710 à 01:00 UTC ; reçue jour 710 à 00:00 + 20 h reste le même jour
        val aTemps = listOf(commande(1, 700, prevueJour = 710), reception(1, 710, 10.0))
        val enRetard = listOf(commande(2, 700, prevueJour = 710), reception(2, 711, 10.0))
        val sans = listOf(commande(3, 700), reception(3, 720, 10.0))
        val s = calcul(aTemps + enRetard + sans)
        assertEquals(50.0, s.ponctualite ?: -1.0, 0.001)
        assertEquals(3, s.nbCommandesMesurees)
    }

    @Test
    fun `pilier ponctualite absent quand aucune date prevue et poids renormalises`() {
        val pieces = (1..3).flatMap { i -> listOf(commande(i.toLong(), 700), reception(i.toLong(), 710, if (i == 3) 11.0 else 10.0)) }
        val s = calcul(pieces)
        assertNull(s.ponctualite)
        // conformité : 2/3 ; seul pilier mesurable => score = 67
        assertEquals(66.666, s.conformite ?: -1.0, 0.01)
        assertEquals(67, s.score)
    }

    @Test
    fun `conformite a plus ou moins deux pour cent`() {
        fun conformite(recue: Double): Double {
            val pieces = parfaites(2) + commande(50, 700) + reception(50, 705, recue)
            return calcul(pieces).conformite ?: -1.0
        }
        assertEquals(100.0, conformite(10.2), 0.001)
        assertEquals(100.0 * 2 / 3, conformite(10.201), 0.01)
    }

    @Test
    fun `prix reel superieur penalise et inferieur ne penalise pas`() {
        val chere = calcul(listOf(commande(1, 700, prix = 100.0), reception(1, 710, 10.0, prixReel = 110.0)))
        // écart 10 % => pénalité 50 => pilier 50
        assertEquals(50.0, chere.prix ?: -1.0, 0.001)
        val moins = calcul(listOf(commande(1, 700, prix = 100.0), reception(1, 710, 10.0, prixReel = 80.0)))
        assertEquals(100.0, moins.prix ?: -1.0, 0.001)
        val sans = calcul(listOf(commande(1, 700), reception(1, 710, 10.0)))
        assertNull(sans.prix)
    }

    @Test
    fun `prix commande a zero est ignore sans exception`() {
        val s = calcul(listOf(commande(1, 700, prix = 0.0), reception(1, 710, 10.0, prixReel = 50.0)))
        assertNull(s.prix)
        assertNotNull(s.conformite)
    }

    @Test
    fun `penalite de prix plafonnee et ponderee par montant`() {
        // ligne 1 : +50 % (pénalité 100, montant 150*10) ; ligne 2 : exact (pénalité 0, montant 100*10)
        val a = listOf(commande(1, 700, prix = 100.0), reception(1, 710, 10.0, prixReel = 150.0))
        val b = listOf(commande(2, 700, prix = 100.0), reception(2, 710, 10.0, prixReel = 100.0))
        val s = calcul(a + b)
        assertEquals(100.0 - 100.0 * 1500.0 / 2500.0, s.prix ?: -1.0, 0.001)
    }

    @Test
    fun `quantites invalides sont ignorees`() {
        val s = calcul(listOf(commande(1, 700, quantite = -5.0), reception(1, 710, 10.0)))
        assertEquals(0, s.nbCommandesMesurees)
        val libre = calcul(listOf(commande(2, 700, produit = null), reception(2, 710, 10.0)))
        assertEquals(0, libre.nbCommandesMesurees)
    }

    @Test
    fun `une facture n est jamais comptee comme une commande`() {
        val facture = piece(
            com.missa.b360.core.domain.model.PurchaseRecordCodec.encode(
                com.missa.b360.core.domain.model.PurchaseRecordPayload(
                    supplierId = fournisseur, supplierName = "F",
                    lines = listOf(com.missa.b360.core.domain.model.PurchaseLine(id = 1, name = "x", unitPrice = 5.0, quantity = 1.0, productId = 55)),
                    subtotal = 5.0, taxRate = 0.0, taxAmount = 0.0, total = 5.0, paymentMethod = "Espèces", paidAmount = 5.0,
                ),
            ),
            700,
        )
        assertEquals(0, calcul(listOf(facture)).nbCommandesMesurees)
    }

    @Test
    fun `pieces non validees ne comptent pas`() {
        val brouillon = commande(1, 700).copy(status = OperationStatus.DRAFT.name)
        assertEquals(0, calcul(listOf(brouillon, reception(1, 710, 10.0))).nbCommandesMesurees)
    }
}
