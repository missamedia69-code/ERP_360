// Fichier de build au niveau du projet.
// Les plugins sont déclarés ici (apply false) et appliqués dans :app
plugins {
    alias(libs.plugins.android.application) apply false
    // Avec AGP 9.0, Kotlin est intégré : pas de plugin org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose) apply false
}
