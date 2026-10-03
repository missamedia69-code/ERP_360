package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.model.ClientImportRow
import javax.inject.Inject

/** Bilan d'un import : tout ce qui n'est pas créé est compté, rien n'échoue en silence. */
data class ClientImportReport(
    val crees: Int,
    /** Téléphone déjà connu d'une fiche existante : ligne ignorée, jamais fusionnée. */
    val doublons: Int,
    val echecs: Int,
    /** `true` si l'import s'est arrêté (licence expirée ou permission refusée). */
    val interrompu: Boolean = false,
)

/**
 * Import partiel : les lignes valides déjà prévisualisées sont créées une à une en fiches
 * « à compléter » ; une ligne refusée n'empêche pas les suivantes. Rien n'est écrit avant l'appel.
 * Les nouvelles fiches n'ont ni vente ni encaissement : leur compte vide équivaut à une ligne
 * `client_balances` absente (le worker de reconstruction crée les lignes manquantes).
 */
class ClientImportUseCase @Inject constructor(
    private val createClient: CreateClientUseCase,
) {
    suspend operator fun invoke(
        lignes: List<ClientImportRow>,
        progression: (Int, Int) -> Unit = { _, _ -> },
    ): ClientImportReport {
        var crees = 0
        var doublons = 0
        var echecs = 0
        lignes.forEachIndexed { index, ligne ->
            val resultat = createClient(
                nom = ligne.nom,
                telephone = ligne.telephone,
                email = ligne.email,
                adresse = ligne.adresse,
                statutInitial = ClientStatus.A_COMPLETER,
            )
            when (resultat) {
                is CreateClientUseCase.Result.Succes -> crees += 1
                CreateClientUseCase.Result.DoublonPotentiel -> doublons += 1
                CreateClientUseCase.Result.LicenceExpiree,
                CreateClientUseCase.Result.PermissionRefusee,
                -> return ClientImportReport(crees, doublons, echecs + (lignes.size - index), interrompu = true)
                else -> echecs += 1
            }
            progression(index + 1, lignes.size)
        }
        return ClientImportReport(crees, doublons, echecs)
    }
}
