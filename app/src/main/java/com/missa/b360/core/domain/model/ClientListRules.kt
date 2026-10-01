package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType

/** Puces de la liste Clients ; chacune porte un compteur. */
enum class ClientListFilter { TOUS, A_RELANCER, BLOQUES, SANS_ACHAT_90J, NOUVEAUX, INCOMPLETS }

enum class ClientListSort { ENCOURS, NOM, DERNIERE_VENTE }

/** Une ligne de la liste : la fiche, son compte dérivé et l'indicateur « à relancer ». */
data class ClientListItem(
    val client: ClientEntity,
    val balance: ClientBalanceEntity?,
    val aRelancer: Boolean = false,
) {
    val encours: Double get() = balance?.encours ?: 0.0
    val enRetard: Double get() = balance?.enRetard ?: 0.0
}

data class ClientListCounters(
    val total: Int = 0,
    val aRelancer: Int = 0,
    val bloques: Int = 0,
    val sansAchat90j: Int = 0,
    val nouveaux: Int = 0,
    val incomplets: Int = 0,
) {
    fun pour(filtre: ClientListFilter): Int = when (filtre) {
        ClientListFilter.TOUS -> total
        ClientListFilter.A_RELANCER -> aRelancer
        ClientListFilter.BLOQUES -> bloques
        ClientListFilter.SANS_ACHAT_90J -> sansAchat90j
        ClientListFilter.NOUVEAUX -> nouveaux
        ClientListFilter.INCOMPLETS -> incomplets
    }
}

/** Recherche, filtres, tri et compteurs de la liste (pur, testé en JVM). */
object ClientListRules {
    const val NOUVEAU_JOURS = 30
    const val SANS_ACHAT_JOURS = 90
    private const val DAY_MS = ClientMetricsRules.DAY_MS
    private val typesProfessionnels = setOf(
        ClientType.ENTREPRISE, ClientType.ADMINISTRATION, ClientType.REVENDEUR, ClientType.GROSSISTE,
        ClientType.DISTRIBUTEUR, ClientType.CLIENT_EXPORT, ClientType.CLIENT_PROJET,
    )

    /** Recherche par nom, code, téléphone (chiffres seuls compris) ou NIF. */
    fun correspond(client: ClientEntity, requete: String): Boolean {
        val q = requete.trim()
        if (q.isEmpty()) return true
        if (client.nom.contains(q, ignoreCase = true) || client.code.contains(q, ignoreCase = true) ||
            client.telephone.contains(q, ignoreCase = true) || client.nif.orEmpty().contains(q, ignoreCase = true)
        ) return true
        val chiffres = q.filter { it.isDigit() }
        return chiffres.length >= 3 && client.telephone.filter { it.isDigit() }.contains(chiffres)
    }

    fun estBloque(client: ClientEntity): Boolean =
        client.statut == ClientStatus.BLOQUE_CREDIT || client.statut == ClientStatus.BLOQUE_ADMINISTRATIF

    fun estIncomplet(client: ClientEntity): Boolean =
        client.statut == ClientStatus.BROUILLON || client.statut == ClientStatus.A_COMPLETER ||
            (client.telephone.isBlank() && client.email.isNullOrBlank()) ||
            (client.type in typesProfessionnels && (client.nif.isNullOrBlank() || client.adresse.isNullOrBlank()))

    fun estNouveau(client: ClientEntity, now: Long): Boolean = client.createdAt >= now - NOUVEAU_JOURS * DAY_MS

    /** Client en service qui n'a rien acheté depuis 90 jours (ou jamais, et inscrit depuis plus de 90 jours). */
    fun sansAchat90j(item: ClientListItem, now: Long): Boolean {
        val client = item.client
        if (client.statut != ClientStatus.ACTIF && client.statut != ClientStatus.SOUS_SURVEILLANCE) return false
        val limite = now - SANS_ACHAT_JOURS * DAY_MS
        val derniere = item.balance?.derniereVenteAt
        return if (derniere != null) derniere < limite else client.createdAt < limite
    }

    fun appartient(item: ClientListItem, filtre: ClientListFilter, now: Long): Boolean = when (filtre) {
        ClientListFilter.TOUS -> true
        ClientListFilter.A_RELANCER -> item.aRelancer
        ClientListFilter.BLOQUES -> estBloque(item.client)
        ClientListFilter.SANS_ACHAT_90J -> sansAchat90j(item, now)
        ClientListFilter.NOUVEAUX -> estNouveau(item.client, now)
        ClientListFilter.INCOMPLETS -> estIncomplet(item.client)
    }

    /**
     * Lignes de la liste : un client, son compte (`client_balances`) et l'indicateur « à relancer »,
     * calculé avec la dernière relance et la promesse ouverte de chacun.
     */
    fun construireItems(
        clients: List<ClientEntity>,
        comptes: Map<Long, ClientBalanceEntity>,
        suivis: List<ClientFollowupEntity>,
        now: Long,
    ): List<ClientListItem> {
        val derniereRelance = HashMap<Long, Long>()
        val promesseOuverte = HashMap<Long, Long>()
        for (suivi in suivis) {
            if (suivi.type == FollowupType.RELANCE) {
                derniereRelance.merge(suivi.clientId, suivi.createdAt) { a, b -> maxOf(a, b) }
            } else if (suivi.type == FollowupType.PROMESSE && suivi.statut == FollowupStatus.OUVERT) {
                suivi.promesseDate?.let { promesseOuverte.merge(suivi.clientId, it) { a, b -> maxOf(a, b) } }
            }
        }
        return clients.map { client ->
            val compte = comptes[client.id]
            ClientListItem(
                client = client,
                balance = compte,
                aRelancer = ClientFollowupRules.aRelancer(
                    enRetard = compte?.enRetard ?: 0.0,
                    promesseOuverteJusquA = promesseOuverte[client.id],
                    derniereRelanceAt = derniereRelance[client.id],
                    now = now,
                ),
            )
        }
    }

    fun filtrer(items: List<ClientListItem>, requete: String, filtre: ClientListFilter, now: Long): List<ClientListItem> =
        items.filter { correspond(it.client, requete) && appartient(it, filtre, now) }

    fun trier(items: List<ClientListItem>, tri: ClientListSort): List<ClientListItem> {
        val parNom = compareBy<ClientListItem, String>(String.CASE_INSENSITIVE_ORDER) { it.client.nom }
        return when (tri) {
            ClientListSort.NOM -> items.sortedWith(parNom)
            ClientListSort.ENCOURS -> items.sortedWith(compareByDescending<ClientListItem> { it.encours }.then(parNom))
            ClientListSort.DERNIERE_VENTE -> items.sortedWith(
                compareByDescending<ClientListItem> { it.balance?.derniereVenteAt ?: Long.MIN_VALUE }.then(parNom),
            )
        }
    }

    fun compteurs(items: List<ClientListItem>, now: Long): ClientListCounters = ClientListCounters(
        total = items.size,
        aRelancer = items.count { it.aRelancer },
        bloques = items.count { estBloque(it.client) },
        sansAchat90j = items.count { sansAchat90j(it, now) },
        nouveaux = items.count { estNouveau(it.client, now) },
        incomplets = items.count { estIncomplet(it.client) },
    )
}
