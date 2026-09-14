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
| **Bloc émetteur sur les pièces** | ✅ | `core/domain/model/MentionsLegales.kt` |
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

### Phase 2 — Mentions légales sur les pièces — **livrée**
`MentionsLegales.depuis(entreprise, …)` assemble le bloc émetteur (raison sociale, adresse,
téléphone, e‑mail puis identifiants légaux préfixés de leur libellé local) une seule fois, pour :

* la facture affichée à l'écran (`InvoicePaper`), dont l'en‑tête portait jusqu'ici la marque de
  l'éditeur au lieu de celle du commerçant ;
* le PDF téléchargé et le PDF d'impression (`SalePrintAdapter`) ;
* le partage et l'e‑mail, qui n'envoyaient qu'une référence et un total ;
* le relevé de compte client en PDF.

Reste à faire dans cette phase : les devis et commandes, les bons de livraison et les pièces
d'achat, ainsi que le rappel du libellé de taxe local (« TVA », « VAT », « GST ») à la place du
libellé figé.

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

## 6. Pack pays (livré)

Sélectionner un pays à l'onboarding applique désormais un **pack complet** : devise officielle,
taux de taxe, indicatif téléphonique, zone fiscale, identifiants légaux attendus et, quand il
existe, le dispositif national de facturation électronique. Aucun de ces éléments n'a de champ
de saisie permanent : ils s'affichent en lecture dans la carte « Pack pays appliqué », et une
option unique — *Modifier manuellement ces éléments* — déplie devise, taux et pays libre.

### Où vit quelle donnée (une seule source par information)

| Donnée | Source unique |
| --- | --- |
| Devise | `Iso4217.deviseDuPays()` — référentiel ISO du système |
| Taux de taxe standard | `Iso4217.TAXES_SUGGEREES` (136 pays) |
| Nature de la taxe, taux réduits, seuil, IS, IR, e‑facturation | `ReferentielPackPays.TABLE` (78 pays) |
| Zone fiscale et identifiants légaux | `ReferentielFiscal` (89 fiches) |
| Indicatif téléphonique | `Iso4217.INDICATIFS_TELEPHONIQUES` |

`PackPays` ne redéclare donc **ni** la devise **ni** le taux standard : la cohérence entre les
tables est vérifiée par `app/src/test/java/com/missa/b360/PackPaysTest.kt`.

### Taux arbitrés après vérification en ligne

| Pays | Retenu | Motif |
| --- | --- | --- |
| Ghana | 20 % | 15 % de TVA + NHIL 2,5 % + GETFund 2,5 % ; la COVID‑19 Levy est abolie au 1er janvier 2026 |
| Niger | 19 % | Taux national confirmé (circulaire DGI n° 004 de 2025), au‑dessus des 18 % harmonisés UEMOA |
| Tchad | 18 % | Taux normal, réduit à 9 % |
| Congo | 18 % | Corrigé (valait 0 %) ; réduit 5 %, effectif 18,9 % avec les centimes additionnels |
| Comores | 10 % | Corrigé (valait 15 %) ; réduits 3 %, 5 % et 7,5 % |
| Brésil | 17 % | ICMS de référence ; CBS 0,9 % et IBS 0,1 % restent symboliques jusqu'en 2027 |
| Monaco | 20 % | Corrigé (valait 0 %) : TVA française appliquée de plein droit |
| États‑Unis | 0 % | Pas de taxe fédérale ; la sales tax dépend de l'État et du comté |

Ajouts au catalogue : Centrafrique 19 %, Guinée équatoriale 15 %, Comores 10 %.
