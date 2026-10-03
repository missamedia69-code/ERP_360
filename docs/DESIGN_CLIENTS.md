# Design du module Clients — refonte « nuit · violet »

Référence de la refonte visuelle du module Clients (octobre 2026), tirée de la maquette « Refonte UI — ERP Mobile · Clients ».
Elle remplace la référence précédente (boutons d'action bleu nuit, violet réservé à la sélection). Les jetons vivent
dans `ClientCouleurs` (`ui/clients/components/ClientDesign.kt`) ; les composants partagés dans `ClientDesign.kt` et
`ClientDesignBlocs.kt`. Aucun écran Clients n'écrit de teinte en dur.

## Principe

- Le **violet** marque les actions principales, les icônes, la sélection et les états actifs.
- Le **bleu nuit** porte l'identité (textes, cartes d'en-tête) et les actions de suivi (« Encaisser », « Ouvrir le compte »).
- **Vert et orange** sont réservés aux statuts métier (statut actif, risque, retard, alerte) ; rouge pour retard et blocage.
- Fond d'écran gris clair, cartes blanches finement contournées ; aucune ombre lourde.

## Palette

| Jeton | Valeur | Usage |
|---|---|---|
| `Nuit` / `Nuit2` | `#172247` / `#24335F` | Textes forts, cartes d'en-tête en dégradé, boutons « suivi » |
| `Violet` / `VioletProfond` / `VioletPale` | `#7B3FE4` / `#682ED0` / `#F1EAFE` | Actions, icônes, onglet actif, filtre actif, barres |
| `Fond` / `Surface` | `#F4F6FA` / `#F8F9FC` | Fond d'écran, bandes et encadrés |
| `Trait` / `TraitFort` | `#E1E5EE` / `#CDD4E0` | Filets des cartes / contours des boutons |
| `Succes` / `SuccesPale` | `#188252` / `#EAF7F0` | Statut actif, risque normal |
| `Alerte` / `AlertePale` | `#A85A12` / `#FFF3E4` | Retard, relance, attention |
| `Neutre` / `NeutreTexte` | `#EDF0F5` / `#59647A` | Pastilles neutres (« À compléter »), compteurs |

## Dimensions

Rayons : cartes 16, cartes d'en-tête 20, boutons 13, feuilles basses 25 (haut). Boutons ≥ 48 dp de zone tactile.
Champs 46–48 dp, grille 2 colonnes. Textes : titre de carte 15 sp gras, valeur chiffrée 14–15 sp, montant du compte 26 sp, libellés 11–12 sp.

## Composants

- `ClientCarte` : carte blanche (rayon 16, filet, ombre légère), cliquable en option.
- `ClientHero` : carte d'en-tête en dégradé nuit avec grand cercle violet translucide (Vue d'ensemble, fiche, compte).
- `ClientSymbole` / `ClientTitreSection` : symbole violet pâle 27 dp et titre de section avec compteur (`ClientCompteur`).
- `ClientPastille` (+ `ClientStatusChip`, `RiskBadge`) : pastilles arrondies de statut et de risque.
- `BoutonClientPlein` / `BoutonClient` : plein violet par défaut, `couleur = ClientCouleurs.Nuit` pour Encaisser et « Ouvrir le compte client » ;
  `BoutonClientDoux` (violet pâle, Promesse de paiement) ; contour blanc pour les actions secondaires.
- `ClientOnglets` : onglets soulignés (texte violet gras + trait violet pour l'onglet courant).
- `ClientPuce` / filtres : pastille arrondie, active = violet plein, compteur dans un rond.
- `ClientAvatar` : carré arrondi en dégradé violet.
- `ClientFeuille` : feuille basse (poignée, titre, sous-titre, croix, contenu, Annuler + action violette) pour la promesse de paiement et l'encaissement.
- `ClientEtatVide` : cadre en pointillés, symbole violet pâle.

## Écrans

1. **Répertoire** : barre haute, carte « Vue d'ensemble » (clients, encours, retard), recherche + bouton de filtres (plein violet s'il y a des filtres actifs),
   filtres rapides, « N clients » + tri, cartes client (avatar, nom, code, téléphone, statut, encours, chevron), bouton « + Nouveau client » en bas.
   Glisser une carte appelle (droite) ou ouvre WhatsApp (gauche).
2. **Fiche** (Activité, Compte, Contacts, Conditions, Notes) : héros nuit (identité, statut, risque, utilisation du crédit), grille 2 × 2 d'indicateurs,
   trois actions carrées (Vendre, Encaisser, Relancer), bande de contact (Appeler, WhatsApp, SMS), onglets soulignés.
3. **Compte client** : héros (reste dû 26 sp, retard, utilisation du crédit), Encaisser (nuit) + Promesse de paiement (doux), relevé PDF,
   balance âgée (barres violettes), factures ouvertes et encaissements avec compteur.
4. **Promesse de paiement** : feuille basse — reste dû, montant promis, échéances en grille 2 × 2 (Aujourd'hui, Dans 3 jours, Dans 7 jours, Choisir une date).
5. **Formulaire client en 5 étapes** (Identité et coordonnées, Fiscalité, Conditions commerciales, Contacts et adresses, Notes) : carte de progression
   (« Étape n sur 5 », barre violette, pastilles numérotées ou cochées, touchables), carte d'étape avec badge violet, pied Précédent/Annuler + Continuer ;
   Enregistrer à la dernière étape ; une validation en échec renvoie à la première étape en erreur. Aucune étape n'est bloquante.

## Formulaires : structure matricielle compacte

Champs en grille 2 colonnes (`MissaRangee`), nom et adresses sur toute la largeur. Les numéros de bloc, les icônes (champ, chevron, croix, indicatif)
et la bordure active des champs suivent le violet via `LocalCouleurSelection`, fourni par `clientsGraph`.

## Passe de fidélité (E2)

Tailles de la maquette reprises : boutons 11 sp ExtraBold, onglets 11 sp (barre fixe au défilement), titres de section 13 sp, texte secondaire 10 sp,
barre haute blanche de 53 dp (`ClientTopBar`), avatar à une lettre, puces 33 dp. Fiche : indicateurs avec icônes, actions de 61 dp, contacts en tuiles ;
Contacts avec « + Contact / + Adresse » ; Notes et états vides en pointillés (`ClientEtatVide`, `ClientEtatVideCompact`).
Compte : reste dû dans le héros, Encaisser (1) + Promesse (1,25), relevé PDF pleine largeur et icône de téléchargement en barre haute, balance âgée en boîtes (barre 4 dp).
Promesse : sous-titre « Client · nom », rappel du reste dû sur fond `#F6F3FC`, boutons 0,75 / 1,25 avec coche. Formulaire : croix en barre haute,
pastilles d'étape carrées de 20 dp (zone tactile 48 dp de haut), notes d'aide aux étapes 4 et 5, boutons « Ajouter » en pointillés (`ClientBoutonAjout`), pied avec icônes.

## À valider sur appareil

Aucune capture n'a été produite hors appareil : les espacements, la lisibilité du bouton « Nouveau client » au-dessus de la barre de modules
et le défilement des onglets sont à contrôler sur écran réel. Les autres modules gardent leur couleur de marque et seront redessinés un par un.
