package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Étape d'un bon de livraison.
 *
 * Le statut vit dans le payload, pas dans le statut de la pièce : une pièce
 * `VALIDATED` désigne un bon émis, qu'il soit encore en préparation ou déjà
 * remis au client. Mélanger les deux rendrait impossible de distinguer une
 * livraison annulée d'une livraison simplement pas encore partie.
 */
enum class EtapeLivraison(val libelleRes: Int, val ordre: Int) {
    A_PREPARER(R.string.liv_etape_a_preparer, 0),
    EXPEDIEE(R.string.liv_etape_expediee, 1),
    LIVREE(R.string.liv_etape_livree, 2),
}

/** Article transporté sur un bon de livraison. */
@Serializable
data class LigneLivraison(
    val produitId: Long = 0,
    val designation: String,
    val quantite: Double,
    val unite: String? = null,
)

/**
 * Détail d'un bon de livraison, conservé avec la pièce `LIVRAISON`.
 *
 * Même approche que la facture : la table de pièces reste générique, le métier
 * vit dans le payload. Aucune migration de base n'est nécessaire.
 */
@Serializable
data class LivraisonPayload(
    val schemaVersion: Int = 1,
    val clientId: Long = 0,
    val clientName: String,
    val adresseLivraison: String? = null,
    val contact: String? = null,
    val transporteur: String? = null,
    val nombreColis: Int = 0,
    val poidsKg: Double = 0.0,
    /** Référence de la commande ou de la facture d'origine. */
    val referenceOrigine: String? = null,
    val lignes: List<LigneLivraison> = emptyList(),
    /** Nom stable de [EtapeLivraison]. */
    val etape: String = EtapeLivraison.A_PREPARER.name,
    /** Date de remise effective, posée au passage en « livrée ». */
    val dateLivraison: Long? = null,
    val note: String? = null,
)

object LivraisonCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: LivraisonPayload): String =
        json.encodeToString(LivraisonPayload.serializer(), payload)

    fun decode(valeur: String?): LivraisonPayload? = valeur?.let {
        runCatching { json.decodeFromString(LivraisonPayload.serializer(), it) }.getOrNull()
    }
}

/** Un bon de livraison prêt pour l'affichage : la pièce et son détail. */
data class BonLivraison(
    val record: OperationRecordEntity,
    val payload: LivraisonPayload,
) {
    val annule: Boolean get() = record.status == OperationStatus.CANCELLED.name
    val etape: EtapeLivraison get() = LivraisonRules.etape(payload.etape)
    val quantiteTotale: Double get() = payload.lignes.sumOf { it.quantite }
}

/** Compteur d'une étape, pour la barre de synthèse. */
data class CompteurEtape(val etape: EtapeLivraison, val nombre: Int)

/**
 * Règles des bons de livraison — sans dépendance Android ni Room.
 */
object LivraisonRules {

    /** Un bon vide n'a aucun sens : il doit désigner un destinataire. */
    fun destinataireValide(nom: String): Boolean = nom.trim().length >= 2

    fun etape(nom: String?): EtapeLivraison =
        EtapeLivraison.entries.firstOrNull { it.name == nom } ?: EtapeLivraison.A_PREPARER

    /**
     * Étape suivante du cycle. Une livraison remise ne bouge plus : il n'y a pas
     * de retour en arrière, la correction passe par une annulation.
     */
    fun etapeSuivante(courante: EtapeLivraison): EtapeLivraison? = when (courante) {
        EtapeLivraison.A_PREPARER -> EtapeLivraison.EXPEDIEE
        EtapeLivraison.EXPEDIEE -> EtapeLivraison.LIVREE
        EtapeLivraison.LIVREE -> null
    }

    /**
     * Applique le passage à l'étape suivante et pose la date de remise au bon
     * moment : c'est elle qui fait foi pour le délai de livraison.
     */
    fun avancer(payload: LivraisonPayload, maintenant: Long): LivraisonPayload? {
        val suivante = etapeSuivante(etape(payload.etape)) ?: return null
        return payload.copy(
            etape = suivante.name,
            dateLivraison = if (suivante == EtapeLivraison.LIVREE) maintenant else null,
        )
    }

    /** Bons triés : ce qui reste à faire d'abord, puis du plus récent au plus ancien. */
    fun trier(bons: List<BonLivraison>): List<BonLivraison> = bons.sortedWith(
        compareBy<BonLivraison> { it.annule }
            .thenBy { it.etape.ordre }
            .thenByDescending { it.record.createdAt },
    )

    /** Effectif par étape, annulés exclus : ils ne sont plus une charge de travail. */
    fun compteurs(bons: List<BonLivraison>): List<CompteurEtape> = bons
        .filterNot { it.annule }
        .groupingBy { it.etape }
        .eachCount()
        .map { (etape, nombre) -> CompteurEtape(etape, nombre) }
        .sortedBy { it.etape.ordre }

    /** Bons encore à traiter — la file de travail du magasinier. */
    fun enCours(bons: List<BonLivraison>): List<BonLivraison> =
        bons.filterNot { it.annule || it.etape == EtapeLivraison.LIVREE }

    /**
     * Délai moyen entre l'émission du bon et la remise, en jours. Null tant
     * qu'aucune livraison n'est arrivée : une moyenne sur zéro élément
     * n'informe personne.
     */
    fun delaiMoyenJours(bons: List<BonLivraison>): Double? {
        val delais = bons
            .filterNot { it.annule }
            .mapNotNull { bon ->
                bon.payload.dateLivraison?.let { remise ->
                    (remise - bon.record.createdAt).coerceAtLeast(0L).toDouble() / JOUR_MS
                }
            }
        return delais.average().takeIf { delais.isNotEmpty() }
    }

    /** Taux de livraisons menées à terme, annulations exclues du dénominateur. */
    fun tauxLivraison(bons: List<BonLivraison>): Double {
        val actifs = bons.filterNot { it.annule }
        if (actifs.isEmpty()) return 0.0
        return actifs.count { it.etape == EtapeLivraison.LIVREE }.toDouble() / actifs.size * 100.0
    }

    private const val JOUR_MS = 86_400_000L
}
