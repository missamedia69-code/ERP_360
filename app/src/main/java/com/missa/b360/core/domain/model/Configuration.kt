package com.missa.b360.core.domain.model

import com.missa.b360.R

/** Les 8 profils d'activité (A–H) — configuration-modulaire-profil-taille (RA-20). */
enum class ProfilActivite(val labelRes: Int) {
    A(R.string.profil_a), // Micro-commerce / indépendant
    B(R.string.profil_b), // Commerce de détail / boutique
    C(R.string.profil_c), // Commerce multi-points (succursales)
    D(R.string.profil_d), // Distribution / grossiste
    E(R.string.profil_e), // Production / industrie légère
    F(R.string.profil_f), // Services & prestations
    G(R.string.profil_g), // BTP / projets
    H(R.string.profil_h), // Groupe multi-activités / multi-sites
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
