package com.missa.b360.core.documents

import com.missa.b360.core.data.datastore.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/** Paramètres documentaires globaux, conservés hors ligne et réutilisés par tous les modules. */
@Singleton
class DocumentSettings @Inject constructor(private val settings: SettingsStore) {
    suspend fun lire(): DocumentOptions = DocumentOptions(
        couleurPrincipale = settings.get(SettingsStore.Keys.DOC_COULEUR_PRINCIPALE)?.toLongOrNull(16)?.toInt()
            ?: DocumentOptions().couleurPrincipale,
        couleurAccent = settings.get(SettingsStore.Keys.DOC_COULEUR_ACCENT)?.toLongOrNull(16)?.toInt()
            ?: DocumentOptions().couleurAccent,
        afficherLogo = settings.get(SettingsStore.Keys.DOC_AFFICHER_LOGO)?.toBooleanStrictOrNull() ?: true,
        afficherMentionMissa = settings.get(SettingsStore.Keys.DOC_MENTION_MISSA)?.toBooleanStrictOrNull() ?: true,
        mentionMissa = settings.get(SettingsStore.Keys.DOC_TEXTE_MISSA)?.take(80)?.ifBlank { null }
            ?: DocumentOptions().mentionMissa,
        afficherMontantEnLettres = settings.get(SettingsStore.Keys.DOC_MONTANT_LETTRES)?.toBooleanStrictOrNull() ?: true,
        localeTag = settings.get(SettingsStore.Keys.LANGUE) ?: "fr",
    )

    suspend fun enregistrer(options: DocumentOptions) {
        settings.set(SettingsStore.Keys.DOC_COULEUR_PRINCIPALE, options.couleurPrincipale.toUInt().toString(16))
        settings.set(SettingsStore.Keys.DOC_COULEUR_ACCENT, options.couleurAccent.toUInt().toString(16))
        settings.set(SettingsStore.Keys.DOC_AFFICHER_LOGO, options.afficherLogo.toString())
        settings.set(SettingsStore.Keys.DOC_MENTION_MISSA, options.afficherMentionMissa.toString())
        settings.set(SettingsStore.Keys.DOC_TEXTE_MISSA, options.mentionMissa.take(80))
        settings.set(SettingsStore.Keys.DOC_MONTANT_LETTRES, options.afficherMontantEnLettres.toString())
    }
}
