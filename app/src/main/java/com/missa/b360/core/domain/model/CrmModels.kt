package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import java.util.Locale

/** Position d'un client dans le cycle commercial. */
enum class SegmentClient(val libelleRes: Int, val ordre: Int) {
    A_RELANCER(R.string.crm_seg_a_relancer, 0),
    PROSPECT(R.string.crm_seg_prospect, 1),
    NOUVEAU(R.string.crm_seg_nouveau, 2),
    FIDELE(R.string.crm_seg_fidele, 3),
    OCCASIONNEL(R.string.crm_seg_occasionnel, 4),
    DORMANT(R.string.crm_seg_dormant, 5),
    INACTIF(R.string.crm_seg_inactif, 6),
}

/** Un client vu par le commercial : son historique et ce qu'il faut en faire. */
data class FicheCrm(
    val client: ClientEntity,
    val chiffreAffaires: Double,
    val nombreAchats: Int,
    val dernierAchat: Long?,
    val segment: SegmentClient,
    /** Jours écoulés depuis le dernier achat ; null si le client n'a jamais acheté. */
    val joursDepuisAchat: Int?,
)

/** Compteur d'un segment, pour la barre de synthèse. */
data class CompteurSegment(val segment: SegmentClient, val nombre: Int)

/**
 * Règles du module CRM — sans dépendance Android ni Room.
 *
 * **Limite assumée du modèle actuel** : une vente ne porte pas l'identifiant du
 * client mais son nom (`counterpart`). Le rapprochement se fait donc sur un nom
 * normalisé — casse, accents d'espacement et espaces multiples neutralisés. Un
 * client renommé après ses achats perdra son historique tant que les pièces ne
 * porteront pas de clé étrangère ; c'est le prix d'un modèle de pièces
 * générique, et cela se corrigera avec la phase « identifiants des tiers ».
 */
object CrmRules {

    /** Un achat de moins de 30 jours fait encore un client « nouveau ». */
    const val JOURS_NOUVEAU = 30

    /** Passé ce délai sans achat, le client mérite un appel. */
    const val JOURS_RELANCE = 60

    /** Au-delà, il est considéré comme perdu de vue. */
    const val JOURS_DORMANT = 120

    /** Un prospect laissé sans suite au-delà de ce délai remonte en relance. */
    const val JOURS_PROSPECT_TIEDE = 14

    /** Nombre d'achats à partir duquel un client est dit fidèle. */
    const val ACHATS_FIDELE = 3

    private const val JOUR_MS = 86_400_000L

    /** Clé de rapprochement entre un client et le nom porté par une pièce. */
    fun cleRapprochement(nom: String?): String = nom.orEmpty()
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

    /**
     * Construit les fiches, triées par urgence commerciale puis par chiffre
     * d'affaires : ce qui appelle une action apparaît en premier.
     */
    fun fiches(
        clients: List<ClientEntity>,
        pieces: List<OperationRecordEntity>,
        maintenant: Long,
    ): List<FicheCrm> {
        val ventes = pieces.filter {
            it.module == OperationModule.VENTE.name &&
                it.status == OperationStatus.VALIDATED.name
        }
        val parClient = ventes.groupBy { cleRapprochement(it.counterpart) }
        return clients.map { client ->
            val lignes = parClient[cleRapprochement(client.nom)].orEmpty()
            val dernier = lignes.maxOfOrNull { it.createdAt }
            val jours = dernier?.let { ((maintenant - it) / JOUR_MS).toInt().coerceAtLeast(0) }
            FicheCrm(
                client = client,
                chiffreAffaires = lignes.sumOf { it.amount ?: 0.0 },
                nombreAchats = lignes.size,
                dernierAchat = dernier,
                segment = segment(client, lignes.size, jours, maintenant),
                joursDepuisAchat = jours,
            )
        }.sortedWith(
            compareBy<FicheCrm> { it.segment.ordre }.thenByDescending { it.chiffreAffaires },
        )
    }

    /**
     * Segment d'un client. L'ordre des cas fait la règle : un compte désactivé
     * l'emporte sur tout le reste, et un client qui a acheté n'est plus un
     * prospect quoi qu'en dise la fiche.
     */
    fun segment(
        client: ClientEntity,
        nombreAchats: Int,
        joursDepuisAchat: Int?,
        maintenant: Long,
    ): SegmentClient {
        if (!client.active) return SegmentClient.INACTIF
        if (nombreAchats == 0) {
            val anciennete = ((maintenant - client.createdAt) / JOUR_MS).toInt()
            return if (anciennete >= JOURS_PROSPECT_TIEDE) {
                SegmentClient.A_RELANCER
            } else {
                SegmentClient.PROSPECT
            }
        }
        val jours = joursDepuisAchat ?: return SegmentClient.PROSPECT
        return when {
            jours >= JOURS_DORMANT -> SegmentClient.DORMANT
            jours >= JOURS_RELANCE -> SegmentClient.A_RELANCER
            jours <= JOURS_NOUVEAU && nombreAchats == 1 -> SegmentClient.NOUVEAU
            nombreAchats >= ACHATS_FIDELE -> SegmentClient.FIDELE
            else -> SegmentClient.OCCASIONNEL
        }
    }

    /** Effectif de chaque segment présent, dans l'ordre d'urgence. */
    fun compteurs(fiches: List<FicheCrm>): List<CompteurSegment> = fiches
        .groupingBy { it.segment }
        .eachCount()
        .map { (segment, nombre) -> CompteurSegment(segment, nombre) }
        .sortedBy { it.segment.ordre }

    /** Meilleurs clients par chiffre d'affaires cumulé. */
    fun top(fiches: List<FicheCrm>, combien: Int = 5): List<FicheCrm> = fiches
        .filter { it.chiffreAffaires > 0 }
        .sortedByDescending { it.chiffreAffaires }
        .take(combien)

    /** Clients et prospects appelant une action commerciale. */
    fun aRelancer(fiches: List<FicheCrm>): List<FicheCrm> = fiches.filter {
        it.segment == SegmentClient.A_RELANCER || it.segment == SegmentClient.DORMANT
    }

    /**
     * Part des fiches ayant donné lieu à au moins un achat. Les comptes
     * désactivés sont exclus : ils fausseraient le taux vers le bas sans qu'on
     * puisse plus rien y faire.
     */
    fun tauxConversion(fiches: List<FicheCrm>): Double {
        val actifs = fiches.filter { it.segment != SegmentClient.INACTIF }
        if (actifs.isEmpty()) return 0.0
        return actifs.count { it.nombreAchats > 0 }.toDouble() / actifs.size * 100.0
    }

    /** Chiffre d'affaires cumulé de toutes les fiches. */
    fun chiffreAffairesTotal(fiches: List<FicheCrm>): Double =
        fiches.sumOf { it.chiffreAffaires }

    /** Panier moyen, sur les seules fiches ayant acheté. */
    fun panierMoyen(fiches: List<FicheCrm>): Double {
        val achats = fiches.sumOf { it.nombreAchats }
        if (achats == 0) return 0.0
        return chiffreAffairesTotal(fiches) / achats
    }
}
