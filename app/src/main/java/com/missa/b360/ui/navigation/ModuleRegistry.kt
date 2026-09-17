package com.missa.b360.ui.navigation

import com.missa.b360.ui.icons.Iv
import com.missa.b360.R
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import androidx.compose.ui.graphics.Color

/**
 * ModuleRegistry (RA-22) — les 14 modules métier de Missa Business 360.
 * Chaque module = un package `ui/...` avec activation dynamique (profil AV/CUSTOM, 9.1).
 * La barre du bas par défaut : Vente · Stock · Clients · Finances + ➕.
 *
 * Ce registre tient lieu d'`UiScreen` de la spec : chacune des 18 entrées est un
 * écran rattaché à un [ModuleCode] (deux écrans peuvent partager un module, ex.
 * ACHATS et FOURNISSEURS ≠ ACH). Les profils, équivalents du `ProfileCode` de la
 * spec, vivent dans `core/domain/model/Configuration.kt` (`ProfilActivite`).
 * `AppModule.visibles(...)` est l'équivalent de `allowedScreens()`.
 */
enum class AppModule(
    val route: String,
    val titleRes: Int,
    val icon: Int,
    val moduleCode: ModuleCode,
    /**
     * Rang de candidature à la barre du bas : 1 = le plus prioritaire, 0 = ne
     * s'y épingle jamais d'office.
     *
     * La barre suit ainsi le pack réellement choisi. Une liste figée donnait
     * « Vente · Stock · Clients » à tout le monde : un prestataire de services
     * ouvrait son application sans y trouver son métier, et devait passer par
     * « Plus ». Les trois premiers modules présents dans le pack occupent les
     * trois places disponibles — l'accueil et « Plus » prennent déjà deux des
     * cinq.
     */
    val prioriteBarre: Int = 0,
    /**
     * Couleur caractéristique du module : identité visuelle stable dans toute
     * l'application (feuille « Plus », onglet actif de la barre, tuiles et
     * cartes de l'accueil). Les teintes sont réparties sur le cercle
     * chromatique pour rester distinctes deux à deux.
     */
    val couleur: Color,
) {
    VENTE("module_vente", R.string.module_vente, Iv.ShoppingCart, ModuleCode.VEN, prioriteBarre = 1, couleur = Color(0xFF1554E8)), 
    STOCK("module_stock", R.string.module_stock, Iv.Inventory2, ModuleCode.STK, prioriteBarre = 2, couleur = Color(0xFF0E9AA7)), 
    CLIENTS("module_clients", R.string.module_clients, Iv.Group, ModuleCode.VEN, prioriteBarre = 7, couleur = Color(0xFF7047E8)), 
    FINANCES("module_finances", R.string.module_finances, Iv.TrendingUp, ModuleCode.CPT, couleur = Color(0xFF20934A)), 
    ACHATS("module_achats", R.string.module_achats, Iv.CartArrowDown, ModuleCode.ACH, couleur = Color(0xFFF28A16)), 
    FOURNISSEURS("module_fournisseurs", R.string.module_fournisseurs, Iv.Handshake, ModuleCode.ACH, couleur = Color(0xFF92400E)), 
    LIVRAISON("module_livraison", R.string.module_livraison, Iv.LocalShipping, ModuleCode.LOG, prioriteBarre = 8, couleur = Color(0xFF0EA5E9)), 
    PRODUCTION("module_production", R.string.module_production, Iv.LineWeight, ModuleCode.PRO, prioriteBarre = 6, couleur = Color(0xFF8B5CF6)), 
    SERVICES("module_services", R.string.module_services, Iv.RequestQuote, ModuleCode.SER, prioriteBarre = 4, couleur = Color(0xFFDB2777)), 
    RH("module_rh", R.string.module_rh, Iv.Person, ModuleCode.RH, prioriteBarre = 9, couleur = Color(0xFFE11D48)), 
    PROJETS("module_projets", R.string.module_projets, Iv.Workspaces, ModuleCode.PRJ, prioriteBarre = 5, couleur = Color(0xFF3B82F6)), 
    COMPTABILITE("module_comptabilite", R.string.module_comptabilite, Iv.Calculator, ModuleCode.CPT, couleur = Color(0xFF475569)), 
    TRESORERIE("module_tresorerie", R.string.module_tresorerie, Iv.Bank, ModuleCode.TRE, prioriteBarre = 3, couleur = Color(0xFF1E3A8A)), 
    CRM("module_crm", R.string.module_crm, Iv.Campaign, ModuleCode.CRM, couleur = Color(0xFFC026D3)), 
    QUALITE("module_qualite", R.string.module_qualite, Iv.QualityBadge, ModuleCode.QUA, couleur = Color(0xFFCA8A04)), 
    MAINTENANCE("module_maintenance", R.string.module_maintenance, Iv.HammerWrench, ModuleCode.MAI, couleur = Color(0xFFB91C1C)), 
    LOGISTIQUE("module_logistique", R.string.module_logistique, Iv.Warehouse, ModuleCode.LOG, couleur = Color(0xFF65A30D)), 
    REPORTING("module_reporting", R.string.module_reporting, Iv.Analytics, ModuleCode.REP, couleur = Color(0xFF0E7490)), 
    ;

    /** Fond doux dérivé de [couleur] pour les pastilles et vignettes. */
    val couleurDouce: Color
        get() = couleur.copy(alpha = 0.12f)

    companion object {

        /** Retourne les modules correspondant à une liste de ModuleCode. */
        fun fromCodes(codes: List<ModuleCode>): List<AppModule> =
            entries.filter { it.moduleCode in codes }

        /**
         * Modules à présenter compte tenu du pack choisi à l'onboarding.
         *
         * Une liste vide signifie « configuration inconnue » — installation
         * antérieure au pack, ou réglage jamais écrit : on montre alors tout.
         * Masquer par défaut priverait l'utilisateur de ses modules sans qu'il
         * comprenne pourquoi.
         */
        fun visibles(actifs: List<ModuleCode>): List<AppModule> =
            if (actifs.isEmpty()) entries.toList() else entries.filter { it.moduleCode in actifs }

        /** Version qui prend l'activation effective du profil (nouveau système) */
        fun visibles(activation: ActivationProfil): List<AppModule> =
            if (activation.modulesActifs.isEmpty()) entries.toList()
            else entries.filter { it.moduleCode in activation.modulesActifs }

        /** Vérifie si un module est actif selon l'activation */
        fun isActif(module: AppModule, activation: ActivationProfil): Boolean =
            activation.isModuleActif(module.moduleCode)

        /**
         * Barre du bas : les modules épinglés par le Propriétaire, à défaut les
         * mieux placés du pack.
         *
         * Le choix d'usine découle du pack et non d'une liste figée : un
         * commerçant obtient Vente et Stock, un prestataire ses Services, un
         * bureau d'études ses Projets. Un épinglage devenu inactif — le pack a
         * changé — est ignoré plutôt que d'ouvrir un écran vide, et les écrans
         * qui masquent la barre n'y sont jamais proposés.
         */
        fun barreBas(actifs: List<ModuleCode>, epingles: List<String> = emptyList()): List<AppModule> {
            val disponibles = visibles(actifs).filter { it !in SANS_BARRE }
            val choisis = epingles.mapNotNull { nom ->
                disponibles.firstOrNull { it.name == nom }
            }
            return choisis.ifEmpty {
                disponibles
                    .filter { it.prioriteBarre > 0 }
                    .sortedBy { it.prioriteBarre }
            }.take(MAX_ONGLETS)
        }

        fun barreBas(activation: ActivationProfil, epingles: List<String> = emptyList()): List<AppModule> {
            if (activation.modulesActifs.isEmpty()) {
                return barreBas(emptyList(), epingles)
            }
            val visibles = visibles(activation)
            // Si l'utilisateur a épinglé (via profil auto), on respecte même SANS_BARRE
            if (epingles.isNotEmpty()) {
                val choisis = epingles.mapNotNull { nom -> visibles.firstOrNull { it.name == nom } }
                if (choisis.isNotEmpty()) return choisis.take(MAX_ONGLETS)
            }
            // Sinon : place directement les modules du profil sur la barre (max 3)
            // Ordre prioritaire demandé : VEN, ACH, STK, TRE, CPT, PRO, SER, PRJ, LOG, CRM, RH, QUA, MAI, REP
            val ordrePrioritaire = listOf(
                ModuleCode.VEN,
                ModuleCode.ACH,
                ModuleCode.STK,
                ModuleCode.TRE,
                ModuleCode.CPT,
                ModuleCode.PRO,
                ModuleCode.SER,
                ModuleCode.PRJ,
                ModuleCode.LOG,
                ModuleCode.CRM,
                ModuleCode.RH,
                ModuleCode.QUA,
                ModuleCode.MAI,
                ModuleCode.REP,
            )
            // Map ModuleCode -> AppModule principal (pour éviter doublons VEN->VENTE+CLIENTS)
            val principalParCode = mapOf(
                ModuleCode.ACH to ACHATS,
                ModuleCode.VEN to VENTE,
                ModuleCode.STK to STOCK,
                ModuleCode.PRO to PRODUCTION,
                ModuleCode.SER to SERVICES,
                ModuleCode.PRJ to PROJETS,
                ModuleCode.RH to RH,
                ModuleCode.CPT to COMPTABILITE,
                ModuleCode.TRE to TRESORERIE,
                ModuleCode.CRM to CRM,
                ModuleCode.QUA to QUALITE,
                ModuleCode.MAI to MAINTENANCE,
                ModuleCode.LOG to LOGISTIQUE,
                ModuleCode.REP to REPORTING,
            )
            val triesCodes = activation.modulesActifs.sortedBy { code ->
                val idx = ordrePrioritaire.indexOf(code)
                if (idx == -1) 99 else idx
            }
            val result = mutableListOf<AppModule>()
            val vus = mutableSetOf<ModuleCode>()
            for (code in triesCodes) {
                if (code in vus) continue
                vus.add(code)
                val app = principalParCode[code] ?: visibles.firstOrNull { it.moduleCode == code } ?: continue
                // On autorise même SANS_BARRE pour le profil (ex: ACHATS)
                if (app.moduleCode in activation.modulesActifs) {
                    result.add(app)
                    if (result.size >= MAX_ONGLETS) break
                }
            }
            // Si moins de 3, complète avec les autres visibles (ex: CLIENTS, FOURNISSEURS)
            if (result.size < MAX_ONGLETS) {
                val complement = visibles.filter { it !in result && it.moduleCode in activation.modulesActifs }
                    .sortedBy { it.prioriteBarre.let { p -> if (p == 0) 99 else p } }
                result.addAll(complement.take(MAX_ONGLETS - result.size))
            }
            return result.take(MAX_ONGLETS)
        }

        /**
         * Modules qu'il est permis d'épingler.
         *
         * Ceux dont l'écran masque la barre en sont exclus : les proposer
         * reviendrait à offrir un onglet qui disparaît dès qu'on l'ouvre.
         */
        fun epinglables(actifs: List<ModuleCode>): List<AppModule> =
            visibles(actifs).filter { it !in SANS_BARRE }

        fun epinglables(activation: ActivationProfil): List<AppModule> =
            epinglables(activation.modulesActifs.toList())

        /** Nombre maximal d'onglets, l'accueil et « Plus » occupant déjà deux places. */
        const val MAX_ONGLETS = 3

        /**
         * Historiquement Achats et Finances masquaient la barre car leur
         * formulaire était en surimpression. Désormais chaque module est une
         * vraie liste : la barre du bas doit rester visible partout, seul le
         * formulaire plein écran la masque via LocalBarreNavigation.
         */
        private val SANS_BARRE: Set<AppModule> = emptySet()

        /**
         * Vrai si la barre de navigation doit rester visible sur cette route.
         *
         * Le critère est la profondeur : les écrans-liste d'un module sont des
         * destinations de premier niveau, les formulaires et les écrans
         * d'administration sont des tâches dont on sort par « retour ».
         */
        fun barreVisibleSur(route: String?): Boolean {
            val racine = route?.substringBefore('?') ?: return false
            val module = entries.firstOrNull { it.route == racine } ?: return false
            return module !in SANS_BARRE
        }

        /**
         * Menu « Plus » : les modules du pack qui ne tiennent pas dans la barre.
         *
         * La barre et ce menu forment une partition du pack — aucun module
         * n'est ni absent des deux, ni présent dans les deux. Épingler un
         * module le retire donc du menu, et le dépingler l'y remet.
         */
        fun secondaires(
            actifs: List<ModuleCode>,
            epingles: List<String> = emptyList(),
        ): List<AppModule> {
            val barre = barreBas(actifs, epingles).toSet()
            return visibles(actifs).filterNot { it in barre }
        }

        fun secondaires(
            activation: ActivationProfil,
            epingles: List<String> = emptyList(),
        ): List<AppModule> {
            val barre = barreBas(activation, epingles)
            val barreCodes = barre.map { it.moduleCode }.toSet()
            val visibles = visibles(activation)
            // Pour Plus, on veut les modules restants du profil, pas tous les AppModule doublons
            // On garde un seul AppModule par ModuleCode restant (le principal)
            val principalParCode = mapOf(
                ModuleCode.ACH to ACHATS,
                ModuleCode.VEN to VENTE,
                ModuleCode.STK to STOCK,
                ModuleCode.PRO to PRODUCTION,
                ModuleCode.SER to SERVICES,
                ModuleCode.PRJ to PROJETS,
                ModuleCode.RH to RH,
                ModuleCode.CPT to COMPTABILITE,
                ModuleCode.TRE to TRESORERIE,
                ModuleCode.CRM to CRM,
                ModuleCode.QUA to QUALITE,
                ModuleCode.MAI to MAINTENANCE,
                ModuleCode.LOG to LOGISTIQUE,
                ModuleCode.REP to REPORTING,
            )
            val restantsCodes = activation.modulesActifs.filterNot { it in barreCodes }
            val result = mutableListOf<AppModule>()
            for (code in restantsCodes) {
                val app = principalParCode[code] ?: visibles.firstOrNull { it.moduleCode == code } ?: continue
                if (app !in barre) result.add(app)
            }
            // Ajoute aussi les AppModule secondaires du même ModuleCode qui sont dans le pack mais pas principaux
            // (ex: CLIENTS pour VEN, FOURNISSEURS pour ACH) s'ils sont actifs et non dans la barre
            val secondairesDoublons = visibles.filter { it.moduleCode in restantsCodes && it !in result && it !in barre }
            result.addAll(secondairesDoublons)
            return result.distinctBy { it.name }
        }
    }
}
