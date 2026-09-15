# Charte Visuelle 3D Unifiée – Missa Business 360

## 🧱 Style général
- **Isométrique minimaliste** : volumes simples, lisibles, sans surcharge
- **Ambiance tech-pro** : lignes nettes, ombres douces, profondeur subtile
- **Équilibre réalisme / abstraction** : évoquer la fonction sans sur-détail
- **Perspective** : 30° constant pour cohérence inter-modules
- **Éclairage** : lumière douce haut-gauche, ombres légères, reflets subtils
- **Socle** : blanc pur #FFFFFF + gris clair #F2F4F7

## 🌈 Palette
| Catégorie | Principale | Accent | Usage |
|---|---|---|---|
| Fond | Blanc #FFFFFF | Gris clair #F2F4F7 | Arrière-plan, panneaux |
| Modules métier | Bleu Missa #007BFF | Cyan doux #5BC0EB | Graphiques, icônes actives |
| Modules support | Gris acier #6C757D | Argent #D9D9D9 | Éléments secondaires |
| KPI | Vert succès #28A745 | Rouge alerte #DC3545 | Indicateurs |

Dominante par module (sur socle blanc commun) :
- 🛒 Ventes : bleu #007BFF
- 📦 Stock : vert #28A745
- ⚙️ Production : orange
- 💰 Compta/Tréso : gris acier + bleu
- 🚚 Logistique : bleu + flèches
- 👥 Clients/CRM : bleu + violet
- 🏭 Fournisseurs/Achats : bleu + vert

## 🧩 Formes
- Graphiques : barres, courbes, flèches ascendantes
- Objets métiers : chariot, palette, boîte, pièce, document, engrenage, camion, badge
- Abstraits : cubes flottants, cercles translucides, lignes de flux

---

## Galerie complète – 14 modules

### Modules déjà présents dans l'app
#### 📦 Stock disponible
Étagères, boîtes, flèches de mouvement – dominante verte
![Stock](charte-3d-stock.png)

#### 🛒 Ventes du jour
Chariot, carte, graphique ascendant – dominante bleu Missa
![Ventes](charte-3d-ventes.png)

#### ⚙️ Production du jour
Machines, engrenages, flux – dominante orange
![Production](charte-3d-production.png)

#### 💰 Comptabilité
Pièces, documents, calculatrice
![Compta](charte-3d-compta.png)

### 10 modules restants (prêts pour évolution)

#### 📥 Achats
Palette entrante, facture fournisseur
![Achats](charte-3d-achats.png)

#### 👤 Clients
Personnes abstraites, fiches clients, courbe
![Clients](charte-3d-clients.png)

#### 🏭 Fournisseurs
Bâtiment usine, boîtes d'approvisionnement
![Fournisseurs](charte-3d-fournisseurs.png)

#### 🚚 Logistique / Livraison
Camion, route, colis, flèches directionnelles
![Logistique](charte-3d-logistique.png)

#### 🛠️ Services
Casque support, outils, calendrier
![Services](charte-3d-services.png)

#### 👥 RH & Paie
Équipe, badges ID, documents RH
![RH](charte-3d-rh.png)

#### 📋 Projets
Kanban, tâches, timeline, progression
![Projets](charte-3d-projets.png)

#### 💬 CRM
Entonnoir pipeline, réseau personnes, bulles chat
![CRM](charte-3d-crm.png)

#### ✅ Qualité
Checklist, loupe, bouclier validé
![Qualité](charte-3d-qualite.png)

#### 🔧 Maintenance
Clé, engrenage, machine avec alerte
![Maintenance](charte-3d-maintenance.png)

#### 📊 Reporting
Barres, camembert, flèche ascendante
![Reporting](charte-3d-reporting.png)

#### 🏦 Trésorerie
Coffre, pièces, billets, flux
![Trésorerie](charte-3d-tresorerie.png)

---

## 🧭 Intégration proposée (quand modules évolueront)

1. **Composant `Illustration3DModule`** réutilisable :
   ```kotlin
   @Composable
   fun Illustration3DModule(module: AppModule, modifier: Modifier)
   ```
   Map `AppModule -> drawable` depuis `docs/charte-3d-*.png` → `res/drawable`

2. **HomeDashboard** : fond léger (alpha 0.06) derrière KPI + empty state illustré

3. **Headers modules** : chaque écran module affiche son illustration en haut

4. **Cohérence** : même socle blanc, angle 30°, lumière haut-gauche, ombres douces

Tous les PNG sont en `docs/charte-3d-*.png` (16 fichiers, ~30MB) prêts à être convertis en WebP pour l'APK.

Tu veux que je crée le composant `Illustration3DModule` et que j'intègre les 4 déjà présents dans l'accueil maintenant ?
