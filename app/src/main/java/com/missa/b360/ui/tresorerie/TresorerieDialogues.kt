package com.missa.b360.ui.tresorerie

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.missa.b360.R
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.SoldeCompte
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.*
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule

private fun iconeType(type: TypeCompteTresorerie): Int = when (type) {
    TypeCompteTresorerie.CAISSE -> Iv.Savings
    TypeCompteTresorerie.BANQUE -> Iv.Bank
    TypeCompteTresorerie.MOBILE_MONEY -> Iv.Smartphone
    TypeCompteTresorerie.CHEQUE_A_ENCAISSER -> Iv.RequestQuote
    TypeCompteTresorerie.CHEQUE_A_PAYER -> Iv.Description
    TypeCompteTresorerie.COMPTE_TRANSIT -> Iv.SwapHoriz
    TypeCompteTresorerie.PORTEFEUILLE_NUMERIQUE -> Iv.Payments
    TypeCompteTresorerie.AUTRE -> Iv.MoreHoriz
}

/** Création d'un compte de trésorerie. */
@Composable
internal fun TreCompteDialogue(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, TypeCompteTresorerie, String, String, String) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeCompteTresorerie.CAISSE) }
    var etablissement by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var soldeInitial by remember { mutableStateOf("") }
    val nomValide = TresorerieRules.libelleValide(nom)

    MissaFormDialogue(
        titre = stringResource(R.string.tre_nouveau_compte),
        icone = Iv.Bank,
        couleur = AppModule.TRESORERIE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = nomValide,
        enCours = enCours,
        onValider = { onValider(nom.trim(), type, etablissement.trim(), numero.trim(), soldeInitial) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 1) {
            MissaChoixTuiles(
                options = TypeCompteTresorerie.entries.map {
                    MissaTuile(it, stringResource(TresorerieRules.libelleType(it)), iconeType(it))
                },
                selection = type,
                onSelection = { type = it },
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 2) {
            MissaChampTexte(nom, { nom = it }, stringResource(R.string.tre_champ_nom), icone = iconeType(type), requis = true)
            // Établissement et numéro n'ont de sens que hors espèces.
            if (type != TypeCompteTresorerie.CAISSE) {
                MissaChampTexte(etablissement, { etablissement = it }, stringResource(R.string.tre_champ_etablissement), icone = Iv.AccountBalance)
                MissaChampTexte(numero, { numero = it }, stringResource(R.string.tre_champ_numero), icone = Iv.Badge, clavier = MissaClavier.MOT_CLE)
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_montants), numero = 3) {
            MissaChampTexte(
                soldeInitial, { soldeInitial = it }, stringResource(R.string.tre_champ_solde_initial),
                icone = Iv.Payments, clavier = MissaClavier.DECIMAL, aide = stringResource(R.string.tre_solde_initial_aide),
            )
        }
    }
}

/** Encaissement ou décaissement sur un compte. */
@Composable
internal fun TreMouvementDialogue(
    comptes: List<CompteTresorerieEntity>,
    sensInitial: SensMouvement,
    enCours: Boolean,
    onFermer: () -> Unit,
    onNouveauCompte: (() -> Unit)? = null,
    onValider: (
        Long,
        SensMouvement,
        String,
        String,
        CategorieTresorerie,
        String,
        String,
        String,
    ) -> Unit,
) {
    var sens by remember { mutableStateOf(sensInitial) }
    var compteId by remember { mutableStateOf(comptes.firstOrNull()?.id ?: 0L) }
    var montant by remember { mutableStateOf("") }
    var libelle by remember { mutableStateOf("") }
    var tiers by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf(TresorerieRules.categoriesPour(sensInitial).first()) }

    LaunchedEffect(comptes) {
        if (compteId == 0L && comptes.isNotEmpty()) {
            compteId = comptes.first().id
        }
    }

    val categories = TresorerieRules.categoriesPour(sens)
    // Changer de sens change la liste des postes : on retombe sur le premier
    // valide plutôt que de conserver une catégorie devenue incohérente. L'effet
    // évite d'écrire dans un état pendant la composition.
    LaunchedEffect(sens) {
        if (categorie !in TresorerieRules.categoriesPour(sens)) {
            categorie = TresorerieRules.categoriesPour(sens).first()
        }
    }

    val saisieValide = TresorerieRules.montantSaisi(montant) != null &&
        TresorerieRules.libelleValide(libelle) &&
        compteId != 0L

    MissaFormDialogue(
        titre = stringResource(if (sens == SensMouvement.IN) R.string.tre_encaissement else R.string.tre_decaissement),
        icone = if (sens == SensMouvement.IN) Iv.TrendingUp else Iv.TrendingDown,
        couleur = AppModule.TRESORERIE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = saisieValide,
        enCours = enCours,
        onValider = { onValider(compteId, sens, montant, libelle.trim(), categorie, tiers.trim(), "", reference.trim()) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 1) {
            MissaChoixTuiles(
                options = listOf(
                    MissaTuile(SensMouvement.IN, stringResource(R.string.tre_sens_entree), Iv.TrendingUp),
                    MissaTuile(SensMouvement.OUT, stringResource(R.string.tre_sens_sortie), Iv.TrendingDown),
                ),
                selection = sens,
                onSelection = { sens = it },
                colonnes = 2,
            )
            MissaChampListe(
                libelle = stringResource(R.string.tre_champ_compte),
                options = comptes.map { compte ->
                    compte.id to "${compte.nom} · ${stringResource(TresorerieRules.libelleType(TresorerieRules.typeCompte(compte.type)))}"
                },
                selection = compteId.takeIf { it != 0L },
                onSelection = { compteId = it },
                icone = Iv.Bank,
                requis = true,
                aide = if (comptes.isEmpty()) stringResource(R.string.tre_creer_compte_invite) else null,
                actionNouveau = onNouveauCompte?.let { stringResource(R.string.tre_nouveau_compte) to it },
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_montants), numero = 2) {
            MissaChampTexte(montant, { montant = it }, stringResource(R.string.tre_champ_montant), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, requis = true)
            MissaChampTexte(libelle, { libelle = it }, stringResource(R.string.tre_champ_libelle), icone = Iv.Description, requis = true)
            MissaChampListe(
                libelle = stringResource(R.string.tre_champ_categorie),
                options = categories.map { it to stringResource(TresorerieRules.libelleCategorie(it)) },
                selection = categorie,
                onSelection = { categorie = it },
                icone = Iv.Category,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 3) {
            MissaRangee {
                MissaChampTexte(tiers, { tiers = it }, stringResource(R.string.tre_champ_tiers), modifier = Modifier.weight(1f), icone = Iv.Person)
                MissaChampTexte(reference, { reference = it }, stringResource(R.string.tre_champ_reference), modifier = Modifier.weight(1f), icone = Iv.Badge, clavier = MissaClavier.MOT_CLE)
            }
        }
    }
}

/** Virement interne entre deux comptes. */
@Composable
internal fun TreVirementDialogue(
    comptes: List<CompteTresorerieEntity>,
    devise: String,
    soldes: List<SoldeCompte>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, Long, String, String) -> Unit,
) {
    var sourceId by remember { mutableStateOf(comptes.firstOrNull()?.id ?: 0L) }
    var destinationId by remember { mutableStateOf(comptes.getOrNull(1)?.id ?: 0L) }
    var montant by remember { mutableStateOf("") }
    val libelleParDefaut = stringResource(R.string.tre_virement_libelle)
    var libelle by remember { mutableStateOf(libelleParDefaut) }

    val valide = sourceId != 0L && destinationId != 0L && sourceId != destinationId &&
        TresorerieRules.montantSaisi(montant) != null &&
        TresorerieRules.libelleValide(libelle)

    fun options(exclu: Long) = comptes.filter { it.id != exclu }.map { compte ->
        val solde = soldes.firstOrNull { it.compte.id == compte.id }?.let { MoneyUtils.format(it.solde, devise) }
        compte.id to (if (solde != null) "${compte.nom} · $solde" else compte.nom)
    }

    MissaFormDialogue(
        titre = stringResource(R.string.tre_virement),
        sousTitre = stringResource(R.string.tre_virement_aide),
        icone = Iv.SwapHoriz,
        couleur = AppModule.TRESORERIE.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = valide,
        enCours = enCours,
        onValider = { onValider(sourceId, destinationId, montant, libelle.trim()) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_comptes), numero = 1) {
            MissaChampListe(
                libelle = stringResource(R.string.tre_champ_source),
                options = options(destinationId),
                selection = sourceId.takeIf { it != 0L },
                onSelection = { sourceId = it },
                icone = Iv.TrendingDown,
                requis = true,
            )
            MissaChampListe(
                libelle = stringResource(R.string.tre_champ_destination),
                options = options(sourceId),
                selection = destinationId.takeIf { it != 0L },
                onSelection = { destinationId = it },
                icone = Iv.TrendingUp,
                requis = true,
            )
        }
        MissaFormSection(titre = stringResource(R.string.form_section_montants), numero = 2) {
            MissaChampTexte(montant, { montant = it }, stringResource(R.string.tre_champ_montant), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, requis = true, suffixe = devise)
            MissaChampTexte(libelle, { libelle = it }, stringResource(R.string.tre_champ_libelle), icone = Iv.Description, requis = true)
        }
    }
}
