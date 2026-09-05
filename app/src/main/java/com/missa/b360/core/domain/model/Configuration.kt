package com.missa.b360.core.domain.model

import com.missa.b360.R

/**
 * Les 14 modules métier de Missa Business 360.
 * Chaque module possède un code, un nom, une description et des sous-éléments détaillés.
 */
enum class ModuleCode(val code: String, val nom: String, val description: String) {
    ACH("ACH", "Achat", "Gestion des approvisionnements"),
    VEN("VEN", "Vente", "Gestion commerciale"),
    STK("STK", "Stock", "Gestion des stocks"),
    PRO("PRO", "Production", "Gestion de la fabrication"),
    SER("SER", "Service", "Gestion des prestations de service"),
    PRJ("PRJ", "Projet", "Gestion de projets"),
    RH("RH", "RH", "Ressources humaines"),
    CPT("CPT", "Comptabilité", "Comptabilité générale"),
    TRE("TRE", "Trésorerie", "Gestion de trésorerie"),
    CRM("CRM", "CRM", "Relation client"),
    QUA("QUA", "Qualité", "Management de la qualité"),
    MAI("MAI", "Maintenance", "Maintenance des équipements"),
    LOG("LOG", "Logistique", "Transport et logistique"),
    REP("REP", "Reporting", "Pilotage et Business Intelligence");
}

/**
 * Sous-éléments détaillés par module.
 * Chaque module possède une liste de sous-éléments qui correspondent
 * aux fonctionnalités spécifiques activables.
 */
object ModuleSousElements {
    val elements: Map<ModuleCode, List<String>> = mapOf(
        ModuleCode.ACH to listOf(
            "Fournisseurs", "Demandes d'achat", "Appels d'offres", "Commandes fournisseurs",
            "Réceptions", "Contrôle qualité entrée", "Factures fournisseurs", "Avoirs fournisseurs",
            "Contrats fournisseurs", "Évaluations fournisseurs"
        ),
        ModuleCode.VEN to listOf(
            "Clients", "Prospects", "Devis", "Commandes clients", "Livraisons",
            "Factures clients", "Avoirs clients", "Retours clients", "Relances clients",
            "Contrats clients", "Conditions de paiement"
        ),
        ModuleCode.STK to listOf(
            "Articles", "Catégories d'articles", "Entrepôts", "Mouvements de stock",
            "Entrées", "Sorties", "Transferts", "Inventaires", "Réservations de stock",
            "Seuils d'alerte", "Valorisation de stock", "Lots / Séries"
        ),
        ModuleCode.PRO to listOf(
            "Nomenclatures", "Gammes de fabrication", "Ordres de fabrication",
            "Planning de production", "Lancements", "Consommations matières",
            "Déclarations de production", "Produits finis", "Sous-traitance", "Coûts de production"
        ),
        ModuleCode.SER to listOf(
            "Catalogue de prestations", "Contrats de service", "Demandes d'intervention",
            "Planning des interventions", "Interventions", "Comptes-rendus d'intervention",
            "Tickets / SAV", "Garanties", "Facturation des prestations"
        ),
        ModuleCode.PRJ to listOf(
            "Projets", "Phases", "Tâches", "Planning projet", "Affectation des ressources",
            "Budgets projet", "Suivi des temps", "Suivi d'avancement", "Documents projet", "Rentabilité projet"
        ),
        ModuleCode.RH to listOf(
            "Employés", "Contrats de travail", "Postes", "Organigramme", "Congés et absences",
            "Notes de frais", "Paie", "Recrutement", "Formations", "Évaluations", "Compétences"
        ),
        ModuleCode.CPT to listOf(
            "Plan comptable", "Journaux comptables", "Écritures comptables", "Grand livre",
            "Balance", "Compte de résultat", "Bilan", "TVA", "Lettrage", "Clôture comptable", "Immobilisations"
        ),
        ModuleCode.TRE to listOf(
            "Comptes bancaires", "Règlements clients", "Règlements fournisseurs", "Échéanciers",
            "Prévisions de trésorerie", "Rapprochements bancaires", "Effets de commerce", "Lignes de crédit"
        ),
        ModuleCode.CRM to listOf(
            "Prospects", "Opportunités", "Pipeline commercial", "Activités",
            "Campagnes marketing", "Scoring", "Historique relation client", "Objectifs commerciaux"
        ),
        ModuleCode.QUA to listOf(
            "Plans de contrôle", "Contrôles qualité", "Non-conformités", "Actions correctives",
            "Audits qualité", "Réclamations clients", "Certifications", "Indicateurs qualité"
        ),
        ModuleCode.MAI to listOf(
            "Équipements", "Parcs machines", "Interventions de maintenance", "Maintenance préventive",
            "Maintenance corrective", "Planning de maintenance", "Pièces détachées",
            "Historique des pannes", "Coûts de maintenance"
        ),
        ModuleCode.LOG to listOf(
            "Expéditions", "Transporteurs", "Bons d'expédition", "Tracking",
            "Emballages", "Coûts logistiques", "Tournées de livraison", "Preuves de livraison"
        ),
        ModuleCode.REP to listOf(
            "Tableaux de bord", "Indicateurs KPI", "Rapports standards", "Rapports personnalisés",
            "Exports", "Analyses multicritères", "Alertes"
        ),
    )

    fun pourModule(module: ModuleCode): List<String> = elements[module] ?: emptyList()
}

/**
 * Les 7 profils d'activité (AV, ASV, APSV, SER, PRJ, FULL, CUSTOM).
 * Chaque profil active un ensemble de modules avec des sous-éléments spécifiques.
 */
enum class ProfilActivite(val labelRes: Int, val description: String) {
    AV(R.string.profil_av, "Négoce simple sans gestion de stock"),
    ASV(R.string.profil_asv, "Négoce classique avec stock et livraisons"),
    APSV(R.string.profil_apsv, "Fabrication / industrie"),
    SER(R.string.profil_ser, "Prestations de service"),
    PRJ(R.string.profil_prj, "Société de projets / ingénierie"),
    FULL(R.string.profil_full, "Tous les modules activés"),
    CUSTOM(R.string.profil_custom, "Activation manuelle par l'utilisateur");
}

/**
 * Configuration des profils : mapping profil → modules + sous-éléments activés.
 */
object ProfilConfiguration {
    val configurations: Map<ProfilActivite, Map<ModuleCode, List<String>?>> = mapOf(
        ProfilActivite.AV to mapOf(
            ModuleCode.ACH to listOf("Fournisseurs", "Commandes fournisseurs", "Factures fournisseurs"),
            ModuleCode.VEN to listOf("Clients", "Devis", "Commandes clients", "Factures clients"),
            ModuleCode.CPT to null,
            ModuleCode.TRE to null,
            ModuleCode.REP to null,
        ),
        ProfilActivite.ASV to mapOf(
            ModuleCode.ACH to listOf("Fournisseurs", "Commandes fournisseurs", "Réceptions", "Factures fournisseurs"),
            ModuleCode.STK to listOf("Articles", "Entrepôts", "Mouvements de stock", "Inventaires"),
            ModuleCode.VEN to listOf("Clients", "Devis", "Commandes clients", "Livraisons", "Factures clients"),
            ModuleCode.CPT to null,
            ModuleCode.TRE to null,
            ModuleCode.LOG to null,
            ModuleCode.REP to null,
        ),
        ProfilActivite.APSV to mapOf(
            ModuleCode.ACH to listOf("Fournisseurs", "Commandes fournisseurs", "Réceptions", "Factures fournisseurs"),
            ModuleCode.PRO to listOf("Nomenclatures", "Ordres de fabrication", "Planning de production", "Consommations matières"),
            ModuleCode.STK to listOf("Articles", "Entrepôts", "Mouvements de stock", "Inventaires", "Lots / Séries"),
            ModuleCode.VEN to listOf("Clients", "Devis", "Commandes clients", "Livraisons", "Factures clients"),
            ModuleCode.QUA to listOf("Contrôles qualité", "Non-conformités"),
            ModuleCode.CPT to null,
            ModuleCode.TRE to null,
            ModuleCode.LOG to null,
            ModuleCode.REP to null,
        ),
        ProfilActivite.SER to mapOf(
            ModuleCode.SER to listOf("Catalogue de prestations", "Contrats de service", "Interventions", "Tickets / SAV"),
            ModuleCode.VEN to listOf("Clients", "Devis", "Commandes clients", "Factures clients"),
            ModuleCode.PRJ to listOf("Projets", "Tâches", "Suivi d'avancement"),
            ModuleCode.CPT to null,
            ModuleCode.TRE to null,
            ModuleCode.REP to null,
        ),
        ProfilActivite.PRJ to mapOf(
            ModuleCode.PRJ to listOf("Projets", "Phases", "Tâches", "Planning projet", "Budgets projet", "Suivi d'avancement"),
            ModuleCode.RH to listOf("Employés", "Suivi des temps"),
            ModuleCode.VEN to listOf("Clients", "Devis", "Commandes clients", "Factures clients"),
            ModuleCode.CPT to null,
            ModuleCode.TRE to null,
            ModuleCode.REP to null,
        ),
        ProfilActivite.FULL to mapOf(
            ModuleCode.ACH to null, ModuleCode.VEN to null, ModuleCode.STK to null,
            ModuleCode.PRO to null, ModuleCode.SER to null, ModuleCode.PRJ to null,
            ModuleCode.RH to null, ModuleCode.CPT to null, ModuleCode.TRE to null,
            ModuleCode.CRM to null, ModuleCode.QUA to null, ModuleCode.MAI to null,
            ModuleCode.LOG to null, ModuleCode.REP to null,
        ),
        ProfilActivite.CUSTOM to emptyMap(),
    )

    /** Retourne les modules activés pour un profil donné. */
    fun modulesPourProfil(profil: ProfilActivite): List<ModuleCode> {
        if (profil == ProfilActivite.FULL) return ModuleCode.entries.toList()
        if (profil == ProfilActivite.CUSTOM) return emptyList()
        return configurations[profil]?.keys?.toList() ?: emptyList()
    }

    /** Retourne les sous-éléments activés pour un module dans un profil donné. */
    fun sousElementsPourModule(profil: ProfilActivite, module: ModuleCode): List<String> {
        if (profil == ProfilActivite.FULL) return ModuleSousElements.pourModule(module)
        val config = configurations[profil] ?: return emptyList()
        val sousElements = config[module] ?: return emptyList()
        return sousElements ?: ModuleSousElements.pourModule(module)
    }
}

/** Les 6 paliers de taille (effectif) — P1 solo → P6 groupe. */
enum class PalierTaille(val labelRes: Int) {
    P1(R.string.palier_p1), // 1
    P2(R.string.palier_p2), // 2–9
    P3(R.string.palier_p3), // 10–49
    P4(R.string.palier_p4), // 50–249
    P5(R.string.palier_p5), // 250–999
    P6(R.string.palier_p6), // 1000+
}
