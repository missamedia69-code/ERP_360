# Fiscalité multi‑zones & identifiants uniques — cadrage ERP 360

Ce document traduit la spécification fonctionnelle « Fiscalité multi‑zones & identifiants
uniques » en une trajectoire réaliste pour **ERP 360**, application Android **hors ligne**,
mono‑entreprise, à base de Room. Il sert de référence pour arbitrer ce qui est implémenté,
ce qui est reporté et ce qui restera hors périmètre.

## 1. Ce que l'application sait déjà faire

| Capacité | État | Emplacement |
|---|---|---|
| Catalogue pays ISO 3166 (~250) | ✅ | `core/util/MoneyUtils.kt` → `Iso4217.paysDisponibles()` |
| Taux de TVA/GST par pays (134 territoires, réf. 2026) | ✅ | `Iso4217.TAXES_SUGGEREES` |
| Devise ISO 4217 verrouillée au premier usage (RA‑19) | ✅ | `EnterpriseEntity.devise`, `EnsureEnterprisePrerequisites` |
| Indicatifs téléphoniques E.164 (245 territoires) | ✅ | `Iso4217.INDICATIFS_TELEPHONIQUES` |
| Taxe unique par défaut sur les pièces | ✅ | `TaxEntity(nom, taux, parDefaut)` |
| Coordonnées + identifiants légaux de l'entreprise | ✅ | `EnterpriseEntity` (Room v8) |
| **Zone fiscale et identifiants légaux par pays** | ✅ | `core/domain/model/ReferentielFiscal.kt` |
| Journal d'audit horodaté | ✅ | `JournalManager` (rétention paramétrable) |

## 2. Lot livré — Phase 1 : identifiants uniques pilotés par le pays

Transposition locale des tables `tax_identifiers` / `tax_identifier_rules` de la
spécification, adaptée à une application mono‑entreprise embarquée.

* `ZoneFiscale` : 12 zones (CEMAC, UEMOA, OHADA, UE, Europe hors UE, CCG, Afrique du Nord,
  Afrique subsaharienne, Amérique du Nord, Amérique latine, Asie‑Pacifique, Autre), avec le
  référentiel comptable dominant (SYSCOHADA pour l'espace OHADA, EN 16931 pour l'UE).
* `RegleIdentifiant` : désignation officielle locale, motif de contrôle, exemple de saisie.
* Table embarquée de **76 pays détaillés** (NIU, NINEA, IFU, ICE, SIRET, TRN, GSTIN, CNPJ,
  RFC, ABN…) et repli par zone pour tous les autres.
* Écran « Informations sur votre entreprise » : les deux champs d'identifiants **changent de
  libellé, d'exemple et de contrôle selon le pays** (Cameroun → NIU + RCCM ; Maroc → IF + ICE ;
  France → N° TVA intracommunautaire + SIRET ; Émirats → TRN + trade licence…), précédés d'un
  bandeau « Zone fiscale : OHADA — CEMAC · SYSCOHADA ».
* Admin › Réglages : mêmes libellés dynamiques, identifiants modifiables après l'onboarding.
* Contrôle de format **non bloquant** : un format inattendu affiche l'exemple attendu sans
  empêcher l'enregistrement (les motifs sont des approximations, jamais des règles légales).

Correspondance avec le modèle de la spécification :

| Spécification | ERP 360 |
|---|---|
| `tax_identifier_rules.country_code` | clé de la table `ReferentielFiscal.TABLE` |
| `identifier_type` / `display_label` | `RegleIdentifiant.libelle` |
| `regex_pattern` | `RegleIdentifiant.motif` (alias `MOTIFS` ou expression brute) |
| `is_mandatory_invoice` | non implémenté (phase 3) |
| `tax_identifiers.identifier_value` | `EnterpriseEntity.numeroFiscal` / `registreCommerce` |
| `validation_status` / `validation_source` | hors périmètre (pas d'API en ligne : VIES, DGI…) |

## 3. Trajectoire proposée

### Phase 2 — Mentions légales sur les pièces
Reprendre le bloc « émetteur » (nom, adresse, téléphone, e‑mail, identifiants légaux, taux et
libellé de taxe) sur les devis, factures, bons de livraison et reçus, avec le vocabulaire de la
zone. Aucune migration nécessaire : les données existent déjà.

### Phase 3 — Identifiants des tiers
Ajouter les identifiants fiscaux aux clients et fournisseurs (statut B2B/B2C, numéro de TVA),
avec les mêmes règles par pays et le caractère obligatoire sur facture (`is_mandatory_invoice`).
Migration Room requise (`clients`, `suppliers`).

### Phase 4 — Multi‑taux de taxe
Passer d'une taxe unique par défaut à un jeu de taux par pays (normal, réduit, zéro, exonéré),
sélectionnable par ligne de pièce, avec motif d'exonération. Impact fort sur les calculs de
vente, achat et reporting.

### Phase 5 — Retenues à la source et acomptes
Retenues sur paiements (prestations, loyers, honoraires), acomptes d'IS et minima fiscaux de
l'espace OHADA, attestations de retenue.

### Phase 6 — Restitutions déclaratives
États d'aide à la déclaration : CA par taux, TVA collectée / déductible / à décaisser, base des
retenues, sur une période choisie, exportables. **Pas de télé‑déclaration** : l'application est
hors ligne.

## 4. Hors périmètre assumé

* Connecteurs e‑facturation temps réel (ZATCA, PINT‑AE, FNE, Chorus Pro, Peppol, NF‑e, CFDI) :
  ils supposent une connectivité permanente, des certificats et un agrément par pays.
* Vérification en ligne des identifiants (VIES, portails DGI).
* Multi‑entités légales et consolidation : l'application est mono‑entreprise (`enterprise.id = 1`).
* Liasses fiscales et états financiers réglementaires (DSF, TAFIRE) : nécessitent une
  comptabilité en partie double complète.
* Nexus / établissement stable, OSS‑IOSS, CRS‑FATCA.

## 5. Règles de maintenance du référentiel

* Un taux ou un identifiant qui change se corrige dans **un seul fichier**
  (`MoneyUtils.kt` pour les taux, `ReferentielFiscal.kt` pour les identifiants).
* Toute désignation officielle locale (NIU, ICE, TRN…) reste **non traduite** : c'est un nom
  propre. Seuls les libellés génériques de repli et les noms de zones sont traduits dans les
  cinq langues.
* Les motifs de contrôle sont volontairement permissifs : mieux vaut accepter une saisie exacte
  inhabituelle que bloquer un utilisateur.
* Couverture vérifiée par `app/src/test/java/com/missa/b360/ReferentielFiscalTest.kt`.
