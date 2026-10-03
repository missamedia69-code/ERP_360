package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurConformiteRulesTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun doc(
        id: Long,
        expiration: Long? = null,
        verification: VerificationStatut = VerificationStatut.A_VERIFIER,
        archive: Boolean = false,
    ) = FournisseurDocumentEntity(
        id = id, fournisseurId = 1, dateExpiration = expiration, verification = verification, archive = archive,
    )

    @Test fun `la synthese compte expires bientot rejetes et a verifier`() {
        val r = FournisseurConformiteRules.resumer(
            listOf(
                doc(1, now - jour),
                doc(2, now + 5 * jour, VerificationStatut.VERIFIE),
                doc(3, now + 20 * jour, VerificationStatut.REJETE),
                doc(4, now + 200 * jour, VerificationStatut.VERIFIE),
                doc(5),
            ),
            now,
        )
        assertEquals(5, r.total)
        assertEquals(1, r.expires)
        assertEquals(2, r.aExpirer)
        assertEquals(1, r.rejetes)
        assertEquals(2, r.aVerifier)
        assertFalse(r.conforme)
    }

    @Test fun `un document archive ne compte plus`() {
        val r = FournisseurConformiteRules.resumer(listOf(doc(1, now - jour, archive = true)), now)
        assertEquals(0, r.total)
        assertTrue(r.conforme)
    }

    @Test fun `le tri place les expires d abord et les sans echeance en dernier`() {
        val tries = FournisseurConformiteRules.trier(
            listOf(doc(1), doc(2, now + 50 * jour), doc(3, now - 3 * jour), doc(4, archive = true)),
        )
        assertEquals(listOf(3L, 2L, 1L), tries.map { it.id })
    }

    @Test fun `les documents a renouveler s arretent a l horizon et passent les expires d abord`() {
        val liste = FournisseurConformiteRules.aRenouveler(
            listOf(doc(1, now + 120 * jour), doc(2, now + 10 * jour), doc(3, now - 4 * jour), doc(4), doc(5, now + 5 * jour, archive = true)),
            now,
        )
        assertEquals(listOf(3L, 2L), liste.map { it.id })
        assertEquals(listOf(3L, 2L, 1L), FournisseurConformiteRules.aRenouveler(listOf(doc(1, now + 120 * jour), doc(2, now + 10 * jour), doc(3, now - 4 * jour)), now, 200).map { it.id })
    }
}
