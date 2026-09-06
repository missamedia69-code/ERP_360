package com.missa.b360.core.domain.model

import com.missa.b360.R

/** Nature d'un module : cœur d'activité ou brique transverse. */
enum class TypeModule { METIER, SUPPORT }

/**
 * Découpe à deux niveaux des 14 modules de Missa Business 360.
 *
 * 1. **Modules métier** (Achat, Stock, Production, Vente, Service, Projet) :
 *    ils décrivent *ce que fait* l'entreprise et sont pilotés par le profil
 *    d'activité (négoce, industrie, services…).
 * 2. **Modules support / socle** (Comptabilité, Trésorerie, Logistique,
 *    Reporting, CRM, RH, Qualité, Maintenance) : transverses, ils s'ajoutent
 *    au profil et se cochent indépendamment.
 *
 * Les modules recommandés pour un profil donné forment un ensemble cohérent :
 * ils sont verrouillés (voir [metierDuPack] et [recommandes]). L'utilisateur
 * n'ajoute que ce dont il a besoin en plus.
 */
object ModulesSocle {

    /** Modules qui définissent le cœur d'activité, dans l'ordre du flux métier. */
    val metier: List<ModuleCode> = listOf(
        ModuleCode.ACH,
        ModuleCode.STK,
        ModuleCode.PRO,
        ModuleCode.VEN,
        ModuleCode.SER,
        ModuleCode.PRJ,
    )

    /** Briques transverses, proposées en complément du profil métier. */
    val support: List<ModuleCode> = listOf(
        ModuleCode.CPT,
        ModuleCode.TRE,
        ModuleCode.LOG,
        ModuleCode.REP,
        ModuleCode.CRM,
        ModuleCode.RH,
        ModuleCode.QUA,
        ModuleCode.MAI,
    )

    fun type(module: ModuleCode): TypeModule =
        if (module in support) TypeModule.SUPPORT else TypeModule.METIER

    /** Ne garde que les modules métier d'une sélection quelconque. */
    fun filtrerMetier(modules: Collection<ModuleCode>): List<ModuleCode> =
        metier.filter { it in modules }

    /** Ne garde que les modules support d'une sélection quelconque. */
    fun filtrerSupport(modules: Collection<ModuleCode>): List<ModuleCode> =
        support.filter { it in modules }

    /**
     * Modules métier inclus d'office par le profil : le cœur du pack, que
     * l'utilisateur ne peut pas décocher — un profil « Achat-Vente » sans
     * module Vente ne veut rien dire.
     *
     * Le profil « Personnalisé » n'a par définition aucun module imposé.
     */
    fun metierDuPack(profil: ProfilActivite?): Set<ModuleCode> = when (profil) {
        null, ProfilActivite.CUSTOM -> emptySet()
        else -> filtrerMetier(ProfilConfiguration.modulesPourProfil(profil)).toSet()
    }

    /**
     * Modules métier réellement retenus : le pack du profil, plus les modules
     * que l'utilisateur a ajoutés de lui-même. En profil « Personnalisé », il
     * n'y a que des ajouts.
     */
    fun metierActifs(profil: ProfilActivite?, personnalises: Collection<ModuleCode>): List<ModuleCode> =
        when (profil) {
            null -> emptyList()
            ProfilActivite.CUSTOM -> filtrerMetier(personnalises)
            else -> {
                val pack = metierDuPack(profil)
                metier.filter { it in pack || it in personnalises }
            }
        }

    /**
     * Options socle proposées par défaut, selon les règles d'activation :
     *
     * - **Comptabilité** : toujours (indispensable quel que soit le métier) ;
     * - **Trésorerie** : dès qu'il y a de l'achat ou de la vente ;
     * - **Logistique** : dès qu'il y a du stock (expéditions, tournées) ;
     * - **Reporting** : toujours (il consomme les données des autres modules) ;
     * - **CRM** : activité commerciale d'au moins 10 personnes ;
     * - **RH** : à partir de 10 salariés, ou dès qu'on suit des temps sur projet ;
     * - **Qualité** et **Maintenance** : dès qu'il y a de la production.
     *
     * Les modules support déjà prévus par le profil restent recommandés.
     *
     * Ce résultat est verrouillé dans l'interface : il constitue le socle du
     * pack, auquel l'utilisateur ajoute librement les autres briques.
     */
    fun recommandes(
        profil: ProfilActivite?,
        palier: PalierTaille?,
        metierRetenus: Collection<ModuleCode>,
    ): Set<ModuleCode> {
        if (profil == null) return emptySet()
        val achat = ModuleCode.ACH in metierRetenus
        val vente = ModuleCode.VEN in metierRetenus
        val stock = ModuleCode.STK in metierRetenus
        val production = ModuleCode.PRO in metierRetenus
        val projet = ModuleCode.PRJ in metierRetenus
        val grandeStructure = palier != null && palier.ordinal >= PalierTaille.P3.ordinal
        val retenus = linkedSetOf(ModuleCode.CPT, ModuleCode.REP)
        if (achat || vente) retenus += ModuleCode.TRE
        if (stock) retenus += ModuleCode.LOG
        if (vente && grandeStructure) retenus += ModuleCode.CRM
        if (grandeStructure || projet) retenus += ModuleCode.RH
        if (production) {
            retenus += ModuleCode.QUA
            retenus += ModuleCode.MAI
        }
        if (profil != ProfilActivite.CUSTOM) {
            retenus += filtrerSupport(ProfilConfiguration.modulesPourProfil(profil))
        }
        return support.filter { it in retenus }.toSet()
    }

    /** Phrase courte décrivant le module (traduite). */
    fun descriptionRes(module: ModuleCode): Int = when (module) {
        ModuleCode.ACH -> R.string.module_desc_ach
        ModuleCode.VEN -> R.string.module_desc_ven
        ModuleCode.STK -> R.string.module_desc_stk
        ModuleCode.PRO -> R.string.module_desc_pro
        ModuleCode.SER -> R.string.module_desc_ser
        ModuleCode.PRJ -> R.string.module_desc_prj
        ModuleCode.RH -> R.string.module_desc_rh
        ModuleCode.CPT -> R.string.module_desc_cpt
        ModuleCode.TRE -> R.string.module_desc_tre
        ModuleCode.CRM -> R.string.module_desc_crm
        ModuleCode.QUA -> R.string.module_desc_qua
        ModuleCode.MAI -> R.string.module_desc_mai
        ModuleCode.LOG -> R.string.module_desc_log
        ModuleCode.REP -> R.string.module_desc_rep
    }
}
