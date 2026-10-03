# Design du module Clients — référence « nuit · gris · violet »

Référence validée par le propriétaire (captures « fiche client » et « liste des clients », 2 octobre 2026).
Elle s'applique à **tout écran, formulaire, feuille et dialogue lié au client** : `ui/clients/*`, bandeau de
crédit dans Ventes (`ClientCreditBanner`), sélection d'un client dans Ventes/Devis, fiches client du CRM et
notifications clients. Les autres modules gardent le design de l'écran « Informations sur votre entreprise »
(voir `docs/CONVENTIONS.md`). Les jetons vivent dans un seul fichier :
`app/src/main/java/com/missa/b360/ui/clients/components/ClientDesign.kt` (`ClientCouleurs`, `BoutonClient`,
`ClientOnglets`, `ClientVideActivite`). **Ne jamais écrire une teinte en dur dans un écran Clients.**

## Principe

Fond blanc, cartes gris bleuté finement contournées, bleu nuit pour l'identité et le texte. **Aucun bouton
n'est bleu nuit ni violet** : tous les boutons sont des tuiles grises à texte nuit en gras (`BoutonClientPlein`,
`BoutonClient`, `ClientCouleurs.Tuile`), ou blancs à contour gris. **Le violet sert uniquement aux éléments sélectionnables, dans leur
état sélectionné** : onglet courant, filtre courant, onglet actif de la barre du bas. Jamais décoratif (ni texte,
ni icône, ni contour, ni fond d'action). Vert, ambre et rouge de risque restent réservés aux états et sont
**toujours accompagnés d'une icône**.

## Palette (`ClientCouleurs`)

| Jeton | Valeur | Usage |
|---|---|---|
| `Nuit` | `#101C43` | texte, avatar, icônes (jamais un fond de bouton) |
| `Violet` / `VioletProfond` | `#7C3AED` / `#5B21B6` | **uniquement** l'élément sélectionné : onglet ou filtre courant, onglet actif de la barre du bas |
| `Carte` / `CarteBord` | `#EAEDF2` / `#D5DAE3` | résumé, en-tête de fiche, cartes clients, cartes de section |
| `Tuile` / `TuileClaire` | `#E3E6EC` / `#EBEDF1` | actions, boutons secondaires pleins |
| `Pastille` | `#DDE1E8` | pastilles neutres (statut, compteurs) |
| `Trait` | `#D5DAE3` | séparateurs, contours des boutons blancs |
| Texte secondaire | `MissaMuted` | libellés, code, téléphone |

## Typographie et dimensions

- Écrans secondaires (compte, relances, activité, prix négociés, bandeau de crédit) : texte 12 à 15 sp,
  remplissage 10 à 12 dp.
- Titre d'écran 19 sp gras (centré sur la fiche, aligné à gauche sur la liste) ; nom du client 16 à 18 sp gras ;
  code, téléphone, libellés de chiffres 11 sp ; valeur d'un chiffre 14 à 15 sp gras ; boutons 12 à 13 sp gras ;
  pastilles et onglets 10 à 12 sp.
- Cartes : rayon 14 à 16 dp, contour 1 dp, aucune ombre ; remplissage 10 à 12 dp ; espacement vertical 6 dp
  entre éléments d'une liste, 8 dp dans une carte.
- Cibles tactiles **≥ 48 dp** (un onglet visuel de 36 à 38 dp est entouré d'une zone cliquable de 48 dp).
- Défilement vertical uniquement ; jamais de tableau horizontal.

## Composants

- **Barre haute** : `ClientTopBar` (fond toujours blanc ; `titreCentre = false` pour la liste).
- **Fiche** : carte d'en-tête (avatar 52 dp, nom, `code · téléphone`, pastilles statut + risque, utilisation du
  crédit, grille 2×2 En cours / En retard / CA 12 mois / Dernière vente) ; grille d'actions 3×2 (« Vendre »
  en gras ; les six en tuiles grises ; icône « SMS » = bulle de message, distincte de
  l'appel) ; onglets (`ClientOnglets`) ; « + Ajouter une note » et « Toute
  l'activité ⌄ » (`BoutonClient`) ; état vide en pointillés (`ClientVideActivite`).
- **Liste** : carte « Résumé » repliable avec « En temps réel » ; recherche + bouton « Filtres » ; pastilles de
  filtre avec compteur ; ligne « N clients » + menu « Trier par … ⌄ » ; carte client dépliable (encours, retard,
  Appeler, WhatsApp) ; bouton flottant gris « + Nouveau client ».
- **Barre de modules** dans Clients : icône au-dessus du libellé pour tous les onglets, carré violet (icône et libellé blancs) pour l'actif.
- **Risque** : `RiskBadge` (contour et icône de la couleur de risque, texte nuit) ; `ClientStatusChip`
  (gris neutre, couleur seulement pour actif, surveillance et blocages ; cadenas pour les blocages).

## Formulaires : structure matricielle compacte

- Un formulaire = cartes `MissaCarteSection` numérotées, toujours ouvertes.
- **Deux colonnes** (`MissaRangee`, `Modifier.weight(1f)`) pour tous les champs courts qui vont ensemble :
  type | e-mail, NIF | type d'identifiant, délai | limite, remise | remise max, catégorie | badge, commercial |
  grille, segment | canal, territoire | compte comptable, fonction | téléphone, adresse | ville, les deux
  interrupteurs TVA.
- **Pleine largeur** pour les champs longs ou composites : nom, téléphone avec indicatif, adresse seule, motif
  d'exonération, conditions de paiement, notes (4 lignes).
- Feuille modale : remplissage 12 dp, espacement 8 dp, titre 18 sp ; bouton principal épinglé en bas (48 dp).
- Création rapide : deux champs seulement (nom, téléphone) ; le reste se complète dans l'édition.

## Hors périmètre de la référence (à valider sur appareil)

Écrans compte, relances, import, création et édition : alignés sur la palette sans maquette dédiée. Aucune
maquette n'existe pour l'arabe (droite à gauche), les états de chargement et d'erreur, ni le risque élevé ou
bloqué : à vérifier visuellement avant de les considérer comme validés.
