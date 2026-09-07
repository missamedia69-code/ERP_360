package com.missa.b360.ui.tresorerie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.FluxPeriode
import com.missa.b360.core.domain.model.SoldeCompte
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.TresorerieUseCases
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * État et actions de l'écran Trésorerie.
 *
 * Les soldes ne sont jamais stockés : ils se recalculent à chaque émission des
 * deux flux (comptes et mouvements). Sur les volumes d'une TPE cela reste
 * instantané, et aucune désynchronisation n'est possible.
 */
@HiltViewModel
class TresorerieViewModel @Inject constructor(
    private val tresorerie: TresorerieUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    /** Retour d'action, consommé puis effacé par l'écran. */
    sealed class Message {
        data object CompteCree : Message()
        data object MouvementEnregistre : Message()
        data object VirementEnregistre : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object NomDejaPris : Message()
        data object ComptesIncoherents : Message()
        data object Erreur : Message()
    }

    data class EtatTresorerie(
        val comptes: List<SoldeCompte> = emptyList(),
        val mouvements: List<MouvementTresorerieEntity> = emptyList(),
        val soldeGlobal: Double = 0.0,
        val fluxDuMois: FluxPeriode = FluxPeriode(0.0, 0.0),
        val repartition: List<Pair<CategorieTresorerie, Double>> = emptyList(),
        val chargement: Boolean = true,
    )

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    /** Anti double-soumission, comme sur les autres formulaires. */
    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    /** Compte sélectionné pour le détail ; null = tous les comptes. */
    private val _compteFiltre = MutableStateFlow<Long?>(null)
    val compteFiltre: StateFlow<Long?> = _compteFiltre

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatTresorerie> = combine(
        tresorerie.observerComptes(),
        tresorerie.observerMouvements(),
        _compteFiltre,
    ) { comptes, mouvements, filtre ->
        val debutMois = debutDuMois()
        val finMois = finDuMois()
        EtatTresorerie(
            comptes = TresorerieRules.soldes(comptes, mouvements),
            mouvements = mouvements.filter { filtre == null || it.compteId == filtre },
            soldeGlobal = TresorerieRules.soldeGlobal(comptes, mouvements),
            fluxDuMois = TresorerieRules.flux(mouvements, debutMois, finMois),
            repartition = TresorerieRules.repartitionSorties(mouvements, debutMois, finMois),
            chargement = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatTresorerie())

    fun filtrerCompte(compteId: Long?) {
        _compteFiltre.value = if (_compteFiltre.value == compteId) null else compteId
    }

    fun effacerMessage() {
        _message.value = null
    }

    fun creerCompte(
        nom: String,
        type: TypeCompteTresorerie,
        etablissement: String,
        numero: String,
        soldeInitialTexte: String,
    ) {
        // Un solde d'ouverture vide vaut zéro ; une saisie illisible est refusée.
        val soldeInitial = when {
            soldeInitialTexte.isBlank() -> 0.0
            else -> TresorerieRules.montantSaisi(soldeInitialTexte) ?: run {
                _message.value = Message.Invalide
                return
            }
        }
        lancer {
            traiter(
                tresorerie.creerCompte(
                    nom = nom,
                    type = type,
                    etablissement = etablissement,
                    numero = numero,
                    soldeInitial = soldeInitial,
                ),
                Message.CompteCree,
            )
        }
    }

    fun basculerActivite(compteId: Long) {
        lancer { traiter(tresorerie.basculerActivite(compteId), Message.CompteCree) }
    }

    fun enregistrerMouvement(
        compteId: Long,
        sens: SensMouvement,
        montantTexte: String,
        libelle: String,
        categorie: CategorieTresorerie,
        tiers: String,
        modePaiement: String,
        reference: String,
        date: Long,
    ) {
        val montant = TresorerieRules.montantSaisi(montantTexte)
        if (montant == null) {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                tresorerie.enregistrerMouvement(
                    TresorerieUseCases.MouvementParams(
                        compteId = compteId,
                        sens = sens,
                        montant = montant,
                        libelle = libelle,
                        categorie = categorie,
                        tiers = tiers,
                        modePaiement = modePaiement,
                        reference = reference,
                        date = date,
                    ),
                ),
                Message.MouvementEnregistre,
            )
        }
    }

    fun virement(sourceId: Long, destinationId: Long, montantTexte: String, libelle: String) {
        val montant = TresorerieRules.montantSaisi(montantTexte)
        if (montant == null) {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                tresorerie.virementInterne(sourceId, destinationId, montant, libelle),
                Message.VirementEnregistre,
            )
        }
    }

    fun basculerRapprochement(mouvementId: Long) {
        lancer { traiter(tresorerie.basculerRapprochement(mouvementId), null) }
    }

    private fun traiter(resultat: TresorerieUseCases.Resultat, succes: Message?) {
        _message.value = when (resultat) {
            is TresorerieUseCases.Resultat.Succes -> succes
            TresorerieUseCases.Resultat.LectureSeule -> Message.LectureSeule
            TresorerieUseCases.Resultat.Invalide -> Message.Invalide
            TresorerieUseCases.Resultat.NomDejaPris -> Message.NomDejaPris
            TresorerieUseCases.Resultat.ComptesIncoherents -> Message.ComptesIncoherents
        }
    }

    private fun lancer(action: suspend () -> Unit) {
        if (_enCours.value) return
        viewModelScope.launch {
            _enCours.value = true
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _message.value = Message.Erreur
            } finally {
                _enCours.value = false
            }
        }
    }

    /** Comptes ouverts uniquement : les seuls qui acceptent une écriture. */
    fun comptesOuverts(etat: EtatTresorerie): List<CompteTresorerieEntity> =
        etat.comptes.map { it.compte }.filter { it.actif }

    private fun debutDuMois(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun finDuMois(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
