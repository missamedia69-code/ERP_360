package com.missa.b360.core.domain.model

/**
 * Ce qu'une sauvegarde doit respecter pour pouvoir être restaurée.
 *
 * La version du schéma servait de plafond, mais elle était recopiée à la main
 * dans une constante, en parallèle de l'annotation `@Database`. Les deux ont
 * divergé sans que rien ne le signale : la constante est restée à 7 quand la
 * base est passée à 17, et toute sauvegarde produite par l'application est
 * devenue impossible à restaurer — y compris la sienne. Le plafond se lit
 * désormais sur la base réellement ouverte, qui ne peut pas mentir.
 */
object RestaurationRules {

    /**
     * Une sauvegarde est restaurable si elle n'est pas plus récente que le
     * schéma courant : Room sait faire monter une base ancienne jusqu'à la
     * version du jour, il ne sait pas redescendre.
     *
     * La version **égale** est acceptée — c'est le cas ordinaire, restaurer une
     * sauvegarde produite par la même version de l'application.
     */
    fun versionAcceptable(versionSauvegarde: Int, versionCourante: Int): Boolean =
        versionSauvegarde <= versionCourante
}
