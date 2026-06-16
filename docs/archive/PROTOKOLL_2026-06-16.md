# FORMIX Protokoll — 16.06.2026 (Hauptsession)

## 1. Rezepte: Chefkoch-Komplettüberarbeitung

### RecipeDetailDialog (FoodComponents.kt)
- Zutaten als Bullet-Liste (`•`), geparsed per Komma/Newline
- Zubereitung in nummerierte Schritte mit Kreis-Badges (Accent), Regex-Split nach `. ` oder Newline
- Scrollbar via LazyColumn

### Defaults.kt — Alle 155 Rezepte
- 5 Batches mit je ~15-20 Rezepten, nach jedem Batch BUILD geprüft
- Jede Zubereitung: 4-6 Schritte mit konkreten Mengen, Temperaturen, Zeiten, Gewürzen
- Kategorien: fit→FITNESS, build→MUSKELAUFBAU, lose→ABNEHMEN

### 35 neue Chefkoch-Rezepte
- **Frühstück (10):** Baked Feta Eggs, Protein-Bounty-Porridge, Magerquark-Keulchen, Protein-Apple-Crumble, Avocado-Bohnen-Toast, Quinoa-Bananen-Porridge, Carrot-Cake-Baked-Oats, Powerfrühstück-Quark-Obst, Omega-3-Quarkcreme, Chia-Frühstück
- **Mittagessen (9):** Römerpfanne, Fitnessdöner, Quinoa-Thunfisch, Zucchini-Lasagne, Muskel-Salat, Kichererbsen-Pfanne, Low-Carb-Buddha-Bowl, Bohnen-Hack-Pfanne, Rote-Bete-Salat
- **Abendessen (10):** Gefüllte Hähnchenbrust, One-Pot-Pasta, Asia-Nudeln, Lachs-Ofengemüse, Käsespätzle, Bauerntopf, Rindergulasch, Hackbällchen-Senf-Sauce, Protein-Pizza, Zucchini-Karotten-Nudeln
- **Training (5):** Kaiserschmarrn, Pancakes, Snickers-Bowl, Muskel-Shake, Frittata

### Kategorie-Badges
- AiSuggestionRow (DayMenu.kt): `[FITNESS]` `[MUSKELAUFBAU]` `[ABNEHMEN]`
- RecipeDetailDialog (FoodComponents.kt): Badge im Dialog-Header
- RecipeCard (Recipes.kt): Badge neben Namen
- Farbe: Accent (#00FF7F), deutsch

---

## 2. Neue Icons (PNGs)

### Quellen
- `Puls.png` (1024x1536) → PULS-Seite (ersetzt Teller+Besteck)
- `statistik.png` (1024x1536) → Statistik-Kachel
- `start-training.png` (1024x1536) → Start Training Hero-Button

### Verarbeitung
- **1. Versuch:** Eigene VectorDrawables (EKG-Linie, Balken, Hantel) → verworfen
- **2. Versuch:** PNGs mit Background-Removal (Threshold 45) → weiche Kanten erzeugten dunkle Ränder
- **3. Versuch:** Hard threshold 75 + Dilate + Color-Filter → immer noch dunkle Fringe
- **4. Versuch:** Harte Kanten + strict color check → OK
- **Final:** User hat transparente Originale geliefert → NUR resize (kein Processing)

### Dichte-Varianten (2x Größe)
| Ordner | Größe |
|--------|-------|
| drawable | 192x192 |
| drawable-mdpi | 96x96 |
| drawable-xhdpi | 192x192 |
| drawable-xxhdpi | 288x288 |
| drawable-xxxhdpi | 384x384 |

### Anzeigegröße
- DashboardGridCard: 120dp
- DashboardActionCard: 100dp

---

## 3. Debug-Onboarding-Skip

### MainActivity.kt
- `BuildConfig.DEBUG` → erstellt Default-Profil + ModuleSelection.BOTH
- Navigiert direkt zu "dashboard", überspringt alle 17 Onboarding-Schritte

---

## 4. Dashboard: PULS & Ring-Entfernung

### Umbenennung
- "Tagesplaner" → **"PULS**" (überall in Dashboard.kt)
- `ic_tagesplaner_vec` → `ic_puls_orig` (PNG)

### Ringe entfernt (Feedback vom Textdokument)
- **DashboardGridCard**: Hintergrund-Circle + Border + Glow-Radial entfernt → nur Image
- **DashboardActionCard**: Äußerer Ring + Innerer Glow entfernt → nur Image
- Icons jetzt freistehend ohne Doppelringe

---

## 5. JarvisBrain-Erweiterungen

### `control/icon_analyzer.py`
- Analysiert PNG-Icons: Bounding Box, Helligkeit, ASCII-Shape
- Erkennt Icon-Typ (EKG, Balken, Hantel etc.)

### `control/trace_icons.py`
- Konvertiert PNG zu Android VectorDrawable XML
- Fallback auf geometrische Icons bei Fehlschlag

---

## Datei-Änderungen

| Datei | Änderung |
|-------|----------|
| `data/Defaults.kt` | 155 Rezepte + 35 neue (Chefkoch-Stil) |
| `ui/FoodComponents.kt` | RecipeDetailDialog: Bullets + Steps + Badge |
| `features/DayMenu.kt` | AiSuggestionRow: Kategorie-Badge |
| `features/Recipes.kt` | RecipeCard: Kategorie-Badge |
| `features/Dashboard.kt` | PULS-Name, PNG-Icons, 2x Größe, Ringe entfernt |
| `ui/FantasyKit.kt` | DashboardActionCard: Ringe entfernt, 2x Icon |
| `MainActivity.kt` | Debug-Skip Onboarding |
| `res/drawable/ic_puls_orig.png` | Neu (Original-PNG) |
| `res/drawable/ic_statistik_orig.png` | Neu (Original-PNG) |
| `res/drawable/ic_trainig_orig.png` | Neu (Original-PNG, aus start-training.png) |
| `res/drawable{-mdpi,-xhdpi,-xxhdpi,-xxxhdpi}/` | Alle 3 Icons in 5 Dichten |
| `control/icon_analyzer.py` | Neu (JarvisBrain) |
| `control/trace_icons.py` | Neu (JarvisBrain) |

---

## Build-Status
- **BUILD SUCCESSFUL** nach jeder Änderung
- JDK 17 (Microsoft), Gradle 8.x
- APK: `app/build/outputs/apk/debug/app-debug.apk`
