package com.missa.b360.core.domain.model

import com.missa.b360.R

/** Manière de présenter la valeur d'un indicateur. */
enum class FormatIndicateur { MONNAIE, NOMBRE, DECIMAL, POURCENT }

/** Sens de lecture : une hausse est-elle une bonne nouvelle ? */
enum class SensIndicateur { HAUT_BON, BAS_BON, NEUTRE }

/**
 * Catalogue des indicateurs du tableau de bord.
 *
 * Chaque indicateur est rattaché au module qui produit la donnée : il n'apparaît
 * que si ce module est actif (voir [Indicateurs.pourModules]). Un profil de
 * négoce ne verra donc jamais « quantité produite », et une société de services
 * ne verra pas « rotation du stock ».
 *
 * @param module module dont la donnée provient
 * @param moduleAussi second module nécessaire (la marge exige achat **et** vente)
 * @param formuleRes explication de la formule, affichée sous la valeur
 */
enum class IndicateurCode(
    val module: ModuleCode,
    val libelleRes: Int,
    val formuleRes: Int,
    val format: FormatIndicateur,
    val sens: SensIndicateur = SensIndicateur.NEUTRE,
    val moduleAussi: ModuleCode? = null,
) {
    // --- Vente ---
    CA_PERIODE(
        module = ModuleCode.VEN,
        libelleRes = R.string.kpi_ca,
        formuleRes = R.string.kpi_ca_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.HAUT_BON,
    ),
    PANIER_MOYEN(
        module = ModuleCode.VEN,
        libelleRes = R.string.kpi_panier,
        formuleRes = R.string.kpi_panier_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.HAUT_BON,
    ),
    DEVIS_ATTENTE(
        module = ModuleCode.VEN,
        libelleRes = R.string.kpi_devis,
        formuleRes = R.string.kpi_devis_f,
        format = FormatIndicateur.MONNAIE,
    ),

    // --- Achat ---
    ACHATS_PERIODE(
        module = ModuleCode.ACH,
        libelleRes = R.string.kpi_achats,
        formuleRes = R.string.kpi_achats_f,
        format = FormatIndicateur.MONNAIE,
    ),

    // --- Comptabilité (transverse) ---
    MARGE_BRUTE(
        module = ModuleCode.CPT,
        libelleRes = R.string.kpi_marge,
        formuleRes = R.string.kpi_marge_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.HAUT_BON,
        moduleAussi = ModuleCode.VEN,
    ),
    TAUX_MARGE(
        module = ModuleCode.CPT,
        libelleRes = R.string.kpi_taux_marge,
        formuleRes = R.string.kpi_taux_marge_f,
        format = FormatIndicateur.POURCENT,
        sens = SensIndicateur.HAUT_BON,
        moduleAussi = ModuleCode.VEN,
    ),
    PIECES_BROUILLON(
        module = ModuleCode.CPT,
        libelleRes = R.string.kpi_brouillons,
        formuleRes = R.string.kpi_brouillons_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),

    // --- Trésorerie ---
    SOLDE_TRESORERIE(
        module = ModuleCode.TRE,
        libelleRes = R.string.kpi_solde,
        formuleRes = R.string.kpi_solde_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.HAUT_BON,
    ),
    ENCAISSEMENTS(
        module = ModuleCode.TRE,
        libelleRes = R.string.kpi_encaissements,
        formuleRes = R.string.kpi_encaissements_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.HAUT_BON,
    ),
    DECAISSEMENTS(
        module = ModuleCode.TRE,
        libelleRes = R.string.kpi_decaissements,
        formuleRes = R.string.kpi_decaissements_f,
        format = FormatIndicateur.MONNAIE,
        sens = SensIndicateur.BAS_BON,
    ),

    // --- Stock ---
    VALEUR_STOCK(
        module = ModuleCode.STK,
        libelleRes = R.string.kpi_valeur_stock,
        formuleRes = R.string.kpi_valeur_stock_f,
        format = FormatIndicateur.MONNAIE,
    ),
    ARTICLES_SOUS_SEUIL(
        module = ModuleCode.STK,
        libelleRes = R.string.kpi_sous_seuil,
        formuleRes = R.string.kpi_sous_seuil_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),
    ARTICLES_DORMANTS(
        module = ModuleCode.STK,
        libelleRes = R.string.kpi_dormants,
        formuleRes = R.string.kpi_dormants_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),
    ROTATION_STOCK(
        module = ModuleCode.STK,
        libelleRes = R.string.kpi_rotation,
        formuleRes = R.string.kpi_rotation_f,
        format = FormatIndicateur.DECIMAL,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Logistique ---
    LIVRAISONS(
        module = ModuleCode.LOG,
        libelleRes = R.string.kpi_livraisons,
        formuleRes = R.string.kpi_livraisons_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Production ---
    ORDRES_FABRICATION(
        module = ModuleCode.PRO,
        libelleRes = R.string.kpi_of,
        formuleRes = R.string.kpi_of_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),
    QUANTITE_PRODUITE(
        module = ModuleCode.PRO,
        libelleRes = R.string.kpi_quantite,
        formuleRes = R.string.kpi_quantite_f,
        format = FormatIndicateur.DECIMAL,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Service ---
    INTERVENTIONS(
        module = ModuleCode.SER,
        libelleRes = R.string.kpi_interventions,
        formuleRes = R.string.kpi_interventions_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Projet ---
    PROJETS_ACTIFS(
        module = ModuleCode.PRJ,
        libelleRes = R.string.kpi_projets,
        formuleRes = R.string.kpi_projets_f,
        format = FormatIndicateur.NOMBRE,
    ),

    // --- RH ---
    EFFECTIF_ACTIF(
        module = ModuleCode.RH,
        libelleRes = R.string.kpi_effectif,
        formuleRes = R.string.kpi_effectif_f,
        format = FormatIndicateur.NOMBRE,
    ),

    // --- CRM ---
    CLIENTS_ACTIFS(
        module = ModuleCode.CRM,
        libelleRes = R.string.kpi_clients,
        formuleRes = R.string.kpi_clients_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),
    NOUVEAUX_CLIENTS(
        module = ModuleCode.CRM,
        libelleRes = R.string.kpi_nouveaux_clients,
        formuleRes = R.string.kpi_nouveaux_clients_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Qualité ---
    NC_OUVERTES(
        module = ModuleCode.QUA,
        libelleRes = R.string.kpi_nc_ouvertes,
        formuleRes = R.string.kpi_nc_ouvertes_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),
    TAUX_RESOLUTION_NC(
        module = ModuleCode.QUA,
        libelleRes = R.string.kpi_taux_resolution,
        formuleRes = R.string.kpi_taux_resolution_f,
        format = FormatIndicateur.POURCENT,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Maintenance ---
    EQUIPEMENTS_EN_RETARD(
        module = ModuleCode.MAI,
        libelleRes = R.string.kpi_equipements_retard,
        formuleRes = R.string.kpi_equipements_retard_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),
    TAUX_PREVENTIF(
        module = ModuleCode.MAI,
        libelleRes = R.string.kpi_taux_preventif,
        formuleRes = R.string.kpi_taux_preventif_f,
        format = FormatIndicateur.POURCENT,
        sens = SensIndicateur.HAUT_BON,
    ),

    // --- Logistique ---
    TRANSFERTS_EN_TRANSIT(
        module = ModuleCode.LOG,
        libelleRes = R.string.kpi_transferts_transit,
        formuleRes = R.string.kpi_transferts_transit_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),

    // --- CRM ---
    CLIENTS_A_RELANCER(
        module = ModuleCode.CRM,
        libelleRes = R.string.kpi_clients_relancer,
        formuleRes = R.string.kpi_clients_relancer_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.BAS_BON,
    ),

    // --- Reporting ---
    PIECES_VALIDEES(
        module = ModuleCode.REP,
        libelleRes = R.string.kpi_pieces,
        formuleRes = R.string.kpi_pieces_f,
        format = FormatIndicateur.NOMBRE,
        sens = SensIndicateur.HAUT_BON,
    ),
}

/**
 * Alertes du tableau de bord : le reporting qui vient vers l'utilisateur plutôt
 * que d'attendre qu'il ouvre un écran. Chacune dépend elle aussi d'un module actif.
 */
enum class AlerteCode(
    val module: ModuleCode,
    val libelleRes: Int,
    val detailRes: Int,
    /** Le détail affiche un montant formaté plutôt qu'un nombre d'éléments. */
    val avecMontant: Boolean = false,
) {
    STOCK_SOUS_SEUIL(ModuleCode.STK, R.string.alerte_seuil, R.string.alerte_seuil_d),
    STOCK_DORMANT(ModuleCode.STK, R.string.alerte_dormant, R.string.alerte_dormant_d),
    TRESORERIE_NEGATIVE(ModuleCode.TRE, R.string.alerte_tresorerie, R.string.alerte_tresorerie_d, true),
    PIECES_A_VALIDER(ModuleCode.CPT, R.string.alerte_brouillons, R.string.alerte_brouillons_d),
    DEVIS_A_RELANCER(ModuleCode.VEN, R.string.alerte_devis, R.string.alerte_devis_d),
    MARGE_FAIBLE(ModuleCode.CPT, R.string.alerte_marge, R.string.alerte_marge_d, true),
    NC_CRITIQUE(ModuleCode.QUA, R.string.alerte_nc, R.string.alerte_nc_d),
    ENTRETIEN_EN_RETARD(ModuleCode.MAI, R.string.alerte_entretien, R.string.alerte_entretien_d),
    TRANSFERT_NON_RECU(ModuleCode.LOG, R.string.alerte_transfert, R.string.alerte_transfert_d),
    CLIENTS_A_RELANCER(ModuleCode.CRM, R.string.alerte_relance, R.string.alerte_relance_d),
}

/** Sélection des indicateurs et alertes pertinents pour les modules actifs. */
object Indicateurs {

    /** Seuil sous lequel le taux de marge déclenche une alerte (en %). */
    const val SEUIL_MARGE_FAIBLE = 10.0

    /** Ancienneté (jours) au-delà de laquelle un devis doit être relancé. */
    const val JOURS_DEVIS_ANCIEN = 15

    /** Ancienneté (jours) sans mouvement qualifiant un article de dormant. */
    const val JOURS_DORMANT = 90

    /** Période d'analyse par défaut du tableau de bord. */
    const val PERIODE_JOURS = 30

    fun pourModules(actifs: Collection<ModuleCode>): List<IndicateurCode> =
        IndicateurCode.entries.filter { indicateur ->
            indicateur.module in actifs &&
                (indicateur.moduleAussi == null || indicateur.moduleAussi in actifs)
        }

    fun alertesPourModules(actifs: Collection<ModuleCode>): List<AlerteCode> =
        AlerteCode.entries.filter { it.module in actifs }

    /** Indicateurs regroupés par module, dans l'ordre d'affichage du catalogue. */
    fun grouperParModule(actifs: Collection<ModuleCode>): List<Pair<ModuleCode, List<IndicateurCode>>> =
        pourModules(actifs)
            .groupBy { it.module }
            .toList()
            .sortedBy { (module, _) -> ModuleCode.entries.indexOf(module) }
}
