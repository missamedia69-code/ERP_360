package com.missa.b360

import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.data.entity.StatutIntervention
import com.missa.b360.core.data.entity.StatutNc
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention
import com.missa.b360.core.domain.model.QualiteMaintenanceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles des modules Qualité et Maintenance. */
class QualiteMaintenanceRulesTest {

    private val maintenant = 1_000_000_000_000L
    private val jour = 86_400_000L
    private var sequence = 0L

    private fun nc(
        gravite: GraviteNc = GraviteNc.MINEURE,
        statut: StatutNc = StatutNc.OUVERTE,
        origine: OrigineNc = OrigineNc.INTERNE,
        cout: Double = 0.0,
        date: Long = maintenant - 5 * jour,
        resolution: Long? = null,
    ) = NonConformiteEntity(
        id = ++sequence,
        date = date,
        titre = "Écart $sequence",
        gravite = gravite.name,
        origine = origine.name,
        statut = statut.name,
        cout = cout,
        dateResolution = resolution,
        createdAt = date,
    )

    private fun equipement(
        id: Long,
        periodicite: Int = 0,
        miseEnService: Long? = null,
    ) = EquipementEntity(
        id = id,
        nom = "Machine $id",
        type = TypeEquipement.MACHINE.name,
        periodiciteJours = periodicite,
        dateMiseEnService = miseEnService,
        createdAt = 0L,
    )

    private fun intervention(
        equipementId: Long,
        type: TypeIntervention,
        ilYaJours: Int,
        cout: Double = 0.0,
        duree: Double = 0.0,
        statut: StatutIntervention = StatutIntervention.REALISEE,
    ) = InterventionEntity(
        id = ++sequence,
        equipementId = equipementId,
        date = maintenant - ilYaJours * jour,
        type = type.name,
        description = "Intervention $sequence",
        cout = cout,
        dureeHeures = duree,
        statut = statut.name,
        createdAt = 0L,
    )

    // --- Qualité ---

    @Test
    fun `le bilan separe les ecarts par statut`() {
        val bilan = QualiteMaintenanceRules.bilan(
            listOf(
                nc(statut = StatutNc.OUVERTE),
                nc(statut = StatutNc.EN_COURS),
                nc(statut = StatutNc.RESOLUE, resolution = maintenant),
                nc(statut = StatutNc.RESOLUE, resolution = maintenant),
            ),
        )
        assertEquals(1, bilan.ouvertes)
        assertEquals(1, bilan.enCours)
        assertEquals(2, bilan.resolues)
        assertEquals(50.0, bilan.tauxResolution, 0.01)
    }

    @Test
    fun `seuls les ecarts critiques non resolus sont signales`() {
        val bilan = QualiteMaintenanceRules.bilan(
            listOf(
                nc(gravite = GraviteNc.CRITIQUE, statut = StatutNc.OUVERTE),
                nc(gravite = GraviteNc.CRITIQUE, statut = StatutNc.RESOLUE, resolution = maintenant),
            ),
        )
        assertEquals(1, bilan.critiques)
    }

    @Test
    fun `le delai moyen se calcule sur les seuls ecarts refermes`() {
        val bilan = QualiteMaintenanceRules.bilan(
            listOf(
                nc(
                    statut = StatutNc.RESOLUE,
                    date = maintenant - 10 * jour,
                    resolution = maintenant - 6 * jour,
                ),
                nc(
                    statut = StatutNc.RESOLUE,
                    date = maintenant - 10 * jour,
                    resolution = maintenant - 4 * jour,
                ),
                nc(statut = StatutNc.OUVERTE),
            ),
        )
        assertEquals(5.0, bilan.delaiMoyenJours!!, 0.01)
    }

    @Test
    fun `sans ecart referme aucun delai n est invente`() {
        val bilan = QualiteMaintenanceRules.bilan(listOf(nc(statut = StatutNc.OUVERTE)))
        assertNull(bilan.delaiMoyenJours)
    }

    @Test
    fun `un registre vide ne divise pas par zero`() {
        val bilan = QualiteMaintenanceRules.bilan(emptyList())
        assertEquals(0, bilan.total)
        assertEquals(0.0, bilan.tauxResolution, 0.001)
    }

    @Test
    fun `la file de traitement classe par gravite puis par anciennete`() {
        val ancienMineur = nc(gravite = GraviteNc.MINEURE, date = maintenant - 100 * jour)
        val recentCritique = nc(gravite = GraviteNc.CRITIQUE, date = maintenant - jour)
        val ancienCritique = nc(gravite = GraviteNc.CRITIQUE, date = maintenant - 50 * jour)
        val file = QualiteMaintenanceRules.aTraiter(
            listOf(ancienMineur, recentCritique, ancienCritique),
        )
        assertEquals(ancienCritique.id, file[0].id)
        assertEquals(recentCritique.id, file[1].id)
        assertEquals(ancienMineur.id, file[2].id)
    }

    @Test
    fun `les ecarts resolus quittent la file de traitement`() {
        val file = QualiteMaintenanceRules.aTraiter(
            listOf(nc(statut = StatutNc.RESOLUE, resolution = maintenant)),
        )
        assertTrue(file.isEmpty())
    }

    @Test
    fun `les origines sont classees par frequence`() {
        val bilan = QualiteMaintenanceRules.bilan(
            listOf(
                nc(origine = OrigineNc.CLIENT),
                nc(origine = OrigineNc.PRODUCTION),
                nc(origine = OrigineNc.PRODUCTION),
            ),
        )
        assertEquals(OrigineNc.PRODUCTION, bilan.parOrigine.first().first)
        assertEquals(2, bilan.parOrigine.first().second)
    }

    // --- Maintenance ---

    @Test
    fun `l echeance suit la derniere intervention preventive`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(equipement(1, periodicite = 30)),
            listOf(intervention(1, TypeIntervention.PREVENTIVE, ilYaJours = 10)),
            maintenant,
        )
        assertEquals(maintenant + 20 * jour, parc.first().prochaineEcheance)
        assertEquals(0, parc.first().joursDeRetard)
    }

    @Test
    fun `une reparation d urgence ne repousse pas le plan preventif`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(equipement(1, periodicite = 30)),
            listOf(
                intervention(1, TypeIntervention.PREVENTIVE, ilYaJours = 40),
                intervention(1, TypeIntervention.CORRECTIVE, ilYaJours = 2),
            ),
            maintenant,
        )
        assertTrue(parc.first().enRetard)
        assertEquals(10, parc.first().joursDeRetard)
    }

    @Test
    fun `sans preventif enregistre l echeance part de la mise en service`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(equipement(1, periodicite = 90, miseEnService = maintenant - 100 * jour)),
            emptyList(),
            maintenant,
        )
        assertEquals(10, parc.first().joursDeRetard)
    }

    @Test
    fun `un equipement sans periodicite n est jamais en retard`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(equipement(1, periodicite = 0, miseEnService = maintenant - 5_000 * jour)),
            emptyList(),
            maintenant,
        )
        assertNull(parc.first().prochaineEcheance)
        assertTrue(!parc.first().enRetard)
    }

    @Test
    fun `une intervention annulee ne compte ni en cout ni en panne`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(equipement(1)),
            listOf(
                intervention(1, TypeIntervention.CORRECTIVE, 5, cout = 90_000.0),
                intervention(
                    1,
                    TypeIntervention.CORRECTIVE,
                    3,
                    cout = 50_000.0,
                    statut = StatutIntervention.ANNULEE,
                ),
            ),
            maintenant,
        )
        assertEquals(1, parc.first().pannes)
        assertEquals(90_000.0, parc.first().coutCumule, 0.001)
    }

    @Test
    fun `les equipements en retard remontent en tete du parc`() {
        val parc = QualiteMaintenanceRules.etatDuParc(
            listOf(
                equipement(1, periodicite = 0),
                equipement(2, periodicite = 10, miseEnService = maintenant - 60 * jour),
            ),
            emptyList(),
            maintenant,
        )
        assertEquals(2L, parc.first().equipement.id)
        assertEquals(1, QualiteMaintenanceRules.enRetard(parc).size)
    }

    @Test
    fun `le taux de preventif mesure la part des entretiens planifies`() {
        val interventions = listOf(
            intervention(1, TypeIntervention.PREVENTIVE, 5),
            intervention(1, TypeIntervention.PREVENTIVE, 10),
            intervention(1, TypeIntervention.CORRECTIVE, 15),
            intervention(1, TypeIntervention.CORRECTIVE, 20, statut = StatutIntervention.ANNULEE),
        )
        assertEquals(66.67, QualiteMaintenanceRules.tauxPreventif(interventions), 0.1)
    }

    @Test
    fun `sans intervention le taux de preventif reste a zero`() {
        assertEquals(0.0, QualiteMaintenanceRules.tauxPreventif(emptyList()), 0.001)
    }

    @Test
    fun `le cout de maintenance se borne a la periode demandee`() {
        val interventions = listOf(
            intervention(1, TypeIntervention.CORRECTIVE, ilYaJours = 5, cout = 10_000.0),
            intervention(1, TypeIntervention.CORRECTIVE, ilYaJours = 400, cout = 90_000.0),
        )
        val cout = QualiteMaintenanceRules.coutMaintenance(
            interventions,
            debut = maintenant - 30 * jour,
            fin = maintenant,
        )
        assertEquals(10_000.0, cout, 0.001)
    }

    @Test
    fun `une valeur inconnue en base retombe sur un defaut sur`() {
        assertEquals(GraviteNc.MINEURE, QualiteMaintenanceRules.gravite("???"))
        assertEquals(OrigineNc.INTERNE, QualiteMaintenanceRules.origine(null))
        assertEquals(StatutNc.OUVERTE, QualiteMaintenanceRules.statutNc("X"))
        assertEquals(TypeEquipement.MACHINE, QualiteMaintenanceRules.typeEquipement(null))
        assertEquals(TypeIntervention.PREVENTIVE, QualiteMaintenanceRules.typeIntervention("X"))
    }

    @Test
    fun `chaque enumeration porte un libelle traduit`() {
        GraviteNc.entries.forEach { assertTrue(QualiteMaintenanceRules.libelleGravite(it) != 0) }
        OrigineNc.entries.forEach { assertTrue(QualiteMaintenanceRules.libelleOrigine(it) != 0) }
        StatutNc.entries.forEach { assertTrue(QualiteMaintenanceRules.libelleStatutNc(it) != 0) }
        TypeEquipement.entries.forEach {
            assertTrue(QualiteMaintenanceRules.libelleTypeEquipement(it) != 0)
        }
        TypeIntervention.entries.forEach {
            assertTrue(QualiteMaintenanceRules.libelleTypeIntervention(it) != 0)
        }
    }
}
