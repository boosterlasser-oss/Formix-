# 3D Body Selector - Architektur & Datenfluss

## KOMPONENTEN-DIAGRAMM

```
┌─────────────────────────────────────────────────────────────────┐
│                    TrainingFlowScreen (Main)                     │
│                                                                   │
│  CHECK_IN Step:                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  [Frage: "Wie fühlst du dich heute?"]                    │   │
│  │  [Textfeld: checkInText]                                 │   │
│  │                                                           │   │
│  │  [Frage: "Was möchtest du heute trainieren?"]            │   │
│  │  ┌──────────────────────────────────────────────────┐    │   │
│  │  │         BodySelector3D Komponente               │    │   │
│  │  │  ┌────────────────────────────────────────────┐  │    │   │
│  │  │  │   [3D Model (SceneView)]                  │  │    │   │
│  │  │  │   - male.glb geladen                      │  │    │   │
│  │  │  │   - 24 Zone-Nodes                         │  │    │   │
│  │  │  │   - Hit-Testing aktiv                     │  │    │   │
│  │  │  │   [Front/Back Button] (Y-Rotation 180°)   │  │    │   │
│  │  │  └────────────────────────────────────────────┘  │    │   │
│  │  │                                                   │    │   │
│  │  │  Selected Zones:                                │    │   │
│  │  │  ┌──────────────────────────────────────────┐  │    │   │
│  │  │  │ [Brust] [Bizeps] [Beine] ✕              │  │    │   │
│  │  │  │ (FilterChips mit Remove-Option)         │  │    │   │
│  │  │  └──────────────────────────────────────────┘  │    │   │
│  │  └──────────────────────────────────────────────────┘    │   │
│  │         ↓ onFocusGroupsChanged Callback                   │   │
│  │         selectedFocusGroups = Set<String>                 │   │
│  │                                                           │   │
│  │  [Button: "Analyse & Start"] (enabled wenn !empty)        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  Beim Klick auf Button:                                           │
│  1. Analysiere checkInText (Stimmung)                             │
│  2. Nutze selectedFocusGroups (Trainings-Fokus)                   │
│  3. Generiere WorkoutPlan                                         │
│  4. Navigiere zu WORKOUT Step                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## DATENFLUSS

```
┌──────────────────────────────────────────────────────────────┐
│ 1. USER INTERACTION                                            │
│                                                                │
│    User tippt auf Körper (z.B. Brust)                         │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. HIT TESTING (BodySelector3D)                               │
│                                                                │
│    SceneView.pickNode(x, y)                                  │
│    → Hit-Result mit node.name = "zone_chest_front"           │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. ZONE TOGGLE                                                │
│                                                                │
│    selectedZones = selectedZones.toggle("zone_chest_front")   │
│    → selectedZones = {"zone_chest_front"}                     │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. MAPPING (ZoneMapper)                                       │
│                                                                │
│    ZoneMapper.mapZoneToFocus("zone_chest_front")              │
│    → returns "Brust"                                          │
│                                                                │
│    selectedZones.map { zone →                                │
│        ZoneMapper.mapZoneToFocus(zone)                       │
│    }.toSet()                                                  │
│    → {"Brust"}                                                │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. STATE UPDATE (Callback)                                    │
│                                                                │
│    onFocusGroupsChanged({"Brust"})                            │
│                                                                │
│    selectedFocusGroups = {"Brust"}                            │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│ 6. UI UPDATE (Compose)                                        │
│                                                                │
│    FilterChip für "Brust" wird angezeigt                      │
│    Button "Analyse & Start" wird enabled                      │
└──────────────────────────────────────────────────────────────┘
```

---

## STATE MANAGEMENT

```
┌─────────────────────────────────────────────────────────┐
│ TrainingFlowScreen State                                │
├─────────────────────────────────────────────────────────┤
│ var selectedFocusGroups: Set<String> = setOf()          │
│   ↑                                                      │
│   └── Zentral State (Single Source of Truth)            │
│       - Wird durch BodySelector3D aktualisiert          │
│       - Wird für Plan-Generierung genutzt               │
│       - Wird für Button-Validierung genutzt             │
└─────────────────────────────────────────────────────────┘
                        ↕ (bidirektional)
┌─────────────────────────────────────────────────────────┐
│ BodySelector3D Component State                           │
├─────────────────────────────────────────────────────────┤
│ var selectedZones: Set<String> = setOf()                │
│   - Intern State (nur für Komponente)                   │
│   - Wird gemappt zu selectedFocusGroups                 │
│   - Wird durch Hit-Testing aktualisiert                 │
│                                                          │
│ var isFrontView: Boolean = true                         │
│   - Steuert Body-Rotation (Front/Back)                  │
│                                                          │
│ var sceneViewRef: SceneView? = null                     │
│   - Reference zu SceneView für Modell-Zugriff           │
└─────────────────────────────────────────────────────────┘
```

---

## ZONE → FOKUSGRUPPE MAPPING

```
┌─────────────────────────┬────────────────────────┐
│ GLB Node-Name (Zone)    │ Fokusgruppe            │
├─────────────────────────┼────────────────────────┤
│ zone_chest_front        │ Brust                  │
│ zone_upper_back         │ Rücken                 │
│ zone_lower_back         │ Rücken                 │
│ zone_abs_front          │ Bauch/Core             │
│ zone_obliques_L         │ Bauch/Core             │
│ zone_obliques_R         │ Bauch/Core             │
│ zone_shoulder_L         │ Schultern              │
│ zone_shoulder_R         │ Schultern              │
│ zone_biceps_L           │ Bizeps                 │
│ zone_biceps_R           │ Bizeps                 │
│ zone_triceps_L          │ Trizeps                │
│ zone_triceps_R          │ Trizeps                │
│ zone_forearm_L          │ Arme                   │
│ zone_forearm_R          │ Arme                   │
│ zone_quads_L            │ Beine                  │
│ zone_quads_R            │ Beine                  │
│ zone_hamstrings_L       │ Beine                  │
│ zone_hamstrings_R       │ Beine                  │
│ zone_calves_L           │ Beine                  │
│ zone_calves_R           │ Beine                  │
│ zone_tibialis_L         │ Beine                  │
│ zone_tibialis_R         │ Beine                  │
│ zone_glutes_L           │ Po/Gesäß               │
│ zone_glutes_R           │ Po/Gesäß               │
│ zone_head               │ Nacken                 │
│ zone_neck               │ Nacken                 │
└─────────────────────────┴────────────────────────┘

Implementierung: ZoneMapper.kt
Funktion: mapZoneToFocus(zoneName: String): String?
```

---

## TRAININGSPLAN-GENERIERUNG

```
┌──────────────────────────────────────────────────┐
│ CHECK_IN Daten                                   │
├──────────────────────────────────────────────────┤
│ • checkInText: String (z.B. "Fit, energisch")    │
│ • selectedFocusGroups: Set<String>               │
│   └─ {"Brust", "Bizeps", "Schultern"}            │
│ • selectedType: TrainingType                     │
│   └─ STRENGTH, CROSSFIT, HOME, BASICS            │
└──────────────────────────────────────────────────┘
                        ↓
                   Analysiere checkInText
                   → DailyModifier (Intensität, Volumen)
                   → SessionMode (PUSH, RECOVERY, SHORT)
                   → FocusMode (PERFORMANCE, TECHNIQUE, ...)
                        ↓
┌──────────────────────────────────────────────────┐
│ PlanGenerator.buildPlan(                          │
│     fitProfile,                                  │
│     selectedType,                                │
│     dailyModifier,                               │
│     dailyFocus = "Brust" (erste aus Gruppe),    │
│     sorenessFocus = ...                          │
│ )                                                │
└──────────────────────────────────────────────────┘
                        ↓
┌──────────────────────────────────────────────────┐
│ Workout Plan generiert                           │
├──────────────────────────────────────────────────┤
│ • Warmup-Übungen                                 │
│ • 5-7 Haupt-Übungen (auf Fokusgruppen optimiert) │
│ • Cooldown-Übungen                               │
│ • Sets, Reps, Gewichte berechnet                 │
└──────────────────────────────────────────────────┘
                        ↓
              Speichere Plan lokal
            Navigiere zu WORKOUT Screen
```

---

## FEHLERBEHANDLUNG

```
┌──────────────────────────────────────────────────┐
│ BodySelector3D - Error Handling                   │
├──────────────────────────────────────────────────┤
│                                                   │
│ Modell-Laden:                                    │
│ try {                                            │
│   loadModelGlb(modelAssetPath) { model → ... }   │
│ } catch (e: Exception) {                         │
│   Log.e("BodySelector3D", "Failed to load", e)   │
│   // Fallback: Zeige Error-Message               │
│ }                                                │
│                                                   │
│ Hit-Testing:                                     │
│ try {                                            │
│   val results = pickNode(x, y)                   │
│   results?.forEach { zone → ... }                │
│ } catch (e: Exception) {                         │
│   Log.w("BodySelector3D", "Hit test error", e)   │
│   // Continue - Kein Crash                       │
│ }                                                │
│                                                   │
│ Rotation:                                        │
│ try {                                            │
│   model.rotation = newRotation                   │
│ } catch (e: Exception) {                         │
│   Log.w("BodySelector3D", "Rotation error", e)   │
│ }                                                │
│                                                   │
│ Cleanup:                                         │
│ try {                                            │
│   sceneViewRef?.onDestroy()                      │
│ } catch (e: Exception) {                         │
│   Log.w("BodySelector3D", "Cleanup error", e)    │
│ }                                                │
└──────────────────────────────────────────────────┘
```

---

## LIFECYCLE

```
┌─────────────────────────────────────────────────────┐
│ CHECK_IN Screen öffnet                              │
└─────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────┐
│ BodySelector3D wird composed                        │
│ • AndroidView wird erstellt                         │
│ • SceneView wird instantiiert                       │
│ • male.glb wird asynchron geladen                   │
│ • selectedZones = emptySet()                        │
│ • isFrontView = true                                │
└─────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────┐
│ User Interaction                                    │
│ • Tippt auf Körperbereiche                          │
│ • Hit-Tests registrieren Taps                       │
│ • selectedZones wird aktualisiert                   │
│ • onFocusGroupsChanged Callback wird aufgerufen     │
│ • selectedFocusGroups wird aktualisiert             │
│ • UI recomposed (FilterChips angezeigt)             │
└─────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────┐
│ User klickt "Analyse & Start"                       │
│ • Training wird gestartet                           │
│ • Navigiere zu WORKOUT Screen                       │
└─────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────┐
│ DisposableEffect wird aufgerufen                    │
│ • sceneViewRef?.onDestroy()                         │
│ • Resources werden freigegeben                      │
│ • Memory wird optimiert                             │
└─────────────────────────────────────────────────────┘
```

---

## DEPENDENCIES & IMPORTS

```kotlin
// BodySelector3D.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView

// ZoneMapper.kt
// (Keine externe Abhängigkeiten - nur Kotlin stdlib)

// TrainingFlowScreen.kt (existierend)
// + BodySelector3D import neu
```

---

## TESTING MATRIX

```
┌──────────────────────┬──────────────┬────────────┐
│ Test-Bereich         │ Status       │ Priorität  │
├──────────────────────┼──────────────┼────────────┤
│ Kompilierung         │ ✅ Passed    │ P0         │
│ APK-Generierung      │ ✅ Passed    │ P0         │
│ Asset-Loading        │ ⏳ Pending   │ P0         │
│ 3D-Rendering         │ ⏳ Pending   │ P0         │
│ Hit-Testing          │ ⏳ Pending   │ P0         │
│ Zone-Mapping         │ ⏳ Pending   │ P1         │
│ State-Sync           │ ⏳ Pending   │ P1         │
│ UI-Update (Chips)    │ ⏳ Pending   │ P1         │
│ Button-Validierung   │ ⏳ Pending   │ P1         │
│ Plan-Generierung     │ ⏳ Pending   │ P1         │
│ Memory-Cleanup       │ ⏳ Pending   │ P2         │
│ Error-Szenarien      │ ⏳ Pending   │ P2         │
└──────────────────────┴──────────────┴────────────┘

P0 = Critical (Deployment-blocking)
P1 = High (Funktionalität)
P2 = Medium (Robustheit)
P3 = Low (Nice-to-have)
```

---

**Diagramm-Status**: ✅ Fertig  
**Letzte Aktualisierung**: 22.02.2026

