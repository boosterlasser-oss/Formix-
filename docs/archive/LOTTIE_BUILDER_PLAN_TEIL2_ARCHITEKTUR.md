# 🏗️ Universal Lottie Builder - TEIL 2: Tool-Architektur & Module

**Datum:** 01.05.2026  
**Zweck:** Architektur-Design für das Universal Lottie Builder Tool  
**Status:** PLANUNGSDOKUMENT

---

## 🎯 ZIEL DIESES DOKUMENTS

Design der **Tool-Architektur**: Welche Module, welche Funktionen, wie arbeiten sie zusammen?

---

## 📦 MODUL-ÜBERSICHT

Das Tool besteht aus **4 Hauptmodulen** + **1 CLI-Interface**:

```
┌─────────────────────────────────────────────────────────┐
│                   CLI INTERFACE                          │
│         (Kommandozeilen-Bedienung)                      │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   ANALYZER   │  │ TRANSFORMER  │  │   BUILDER    │
│              │  │              │  │              │
│ Analysiert   │  │ Führt        │  │ Intelligente │
│ Lottie-      │  │ Transforma-  │  │ Übungs-      │
│ Struktur     │  │ tionen aus   │  │ Erstellung   │
└──────────────┘  └──────────────┘  └──────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          ▼
                  ┌──────────────┐
                  │     CORE     │
                  │              │
                  │ JSON Loader  │
                  │ JSON Saver   │
                  │ Validator    │
                  └──────────────┘
```

---

## 🧩 MODUL 1: CORE (Basis-Funktionen)

### Zweck:
Grundlegende JSON-Operationen (Laden, Speichern, Validieren)

### Funktionen:

```python
class LottieCore:
    
    def load_json(filepath: str) -> dict:
        """
        Lädt Lottie JSON-Datei
        
        Input:  Dateipfad
        Output: Python Dict mit Lottie-Daten
        Fehler: FileNotFoundError, JSONDecodeError
        """
    
    def save_json(filepath: str, data: dict, backup: bool = True) -> bool:
        """
        Speichert Lottie JSON-Datei
        
        Input:  Dateipfad, Daten, Backup-Flag
        Output: True/False (Erfolg)
        Action: Erstellt .bak Backup wenn backup=True
        """
    
    def validate_lottie(data: dict) -> tuple[bool, str]:
        """
        Validiert ob JSON eine gültige Lottie-Animation ist
        
        Input:  Lottie-Daten
        Output: (is_valid: bool, error_message: str)
        Check:  Pflichtfelder (v, fr, w, h, layers)
        """
    
    def get_metadata(data: dict) -> dict:
        """
        Extrahiert Metadata
        
        Output: {
            "version": "5.8.1",
            "name": "Animation",
            "width": 1934,
            "height": 1562,
            "fps": 29.97,
            "duration_frames": 180,
            "duration_seconds": 6.0,
            "num_layers": 20
        }
        """
```

### Technische Details:
- **Python 3.12+** (schon installiert ✅)
- **Standard Library** (json, pathlib, copy)
- **UTF-8 Encoding** (für Wezterm-Kompatibilität)

---

## 🔍 MODUL 2: ANALYZER (Analyse-Funktionen)

### Zweck:
Analysiert Lottie-Animationen (Struktur, Bewegung, Layer)

### Funktionen:

```python
class LottieAnalyzer:
    
    def analyze_structure(data: dict) -> dict:
        """
        Analysiert Layer-Hierarchie
        
        Output: {
            "total_layers": 20,
            "shape_layers": 15,
            "null_layers": 3,
            "precomp_layers": 2,
            "parent_child_pairs": [(1,2), (1,3), (4,5)],
            "root_layers": [1, 4, 10]  # Keine Parents
        }
        """
    
    def analyze_animation(data: dict) -> dict:
        """
        Analysiert Bewegungstyp
        
        Output: {
            "animated_properties": ["position", "rotation", "scale"],
            "has_position_anim": True,
            "has_rotation_anim": True,
            "has_scale_anim": False,
            "movement_direction": "vertical",  # up/down/left/right/complex
            "is_loopable": True,
            "complexity_score": 7.5  # 0-10
        }
        """
    
    def find_layers_by_name(data: dict, pattern: str) -> list:
        """
        Findet Layers nach Namen (Regex)
        
        Input:  "arm.*" oder "leg"
        Output: [layer_index_1, layer_index_2, ...]
        """
    
    def get_color_palette(data: dict) -> list:
        """
        Extrahiert alle Farben
        
        Output: [
            {"color": [0.5, 0.2, 0.8, 1.0], "count": 5, "layers": [1,3,5]},
            {"color": [0.9, 0.6, 0.4, 1.0], "count": 3, "layers": [2,4]}
        ]
        """
```

### Warum wichtig?
- Hilft bei **Similarity-Search** (ähnliche Animationen finden)
- **Automatische Strategie-Wahl** für Transformationen
- **Debugging** wenn Transformation fehlschlägt

---

## 🔧 MODUL 3: TRANSFORMER (Transformations-Funktionen)

### Zweck:
Führt Transformationen auf Lottie-Daten aus (das Herzstück!)

### Funktionen:

```python
class LottieTransformer:
    
    # ===== BASIC TRANSFORMATIONS (Einfach) =====
    
    def mirror(data: dict, axis: str = "horizontal") -> dict:
        """
        Spiegelt Animation
        
        Input:  axis = "horizontal" oder "vertical"
        Action: Ändert Scale-Property aller Root-Layers
                horizontal: [100,100,100] → [-100,100,100]
                vertical:   [100,100,100] → [100,-100,100]
        Output: Transformierte Daten
        """
    
    def rotate(data: dict, degrees: float) -> dict:
        """
        Rotiert Animation um X Grad
        
        Input:  degrees (z.B. 90, 180, -45)
        Action: Addiert Grad zu allen Rotation-Properties
                Statisch: r.k += degrees
                Animiert: r.k[i].s[0] += degrees für jedes Keyframe
        Output: Transformierte Daten
        """
    
    def scale(data: dict, factor: float, maintain_aspect: bool = True) -> dict:
        """
        Skaliert Animation
        
        Input:  factor (z.B. 1.5 = 150%, 0.5 = 50%)
                maintain_aspect = True (proportional)
        Action: data["w"] *= factor
                data["h"] *= factor
        Output: Transformierte Daten
        """
    
    def speed(data: dict, multiplier: float) -> dict:
        """
        Ändert Geschwindigkeit
        
        Input:  multiplier (z.B. 2.0 = doppelt so schnell, 0.5 = halb)
        Action: data["fr"] *= multiplier
                data["op"] /= multiplier
                (ODER: Alle Keyframe-Timings anpassen)
        Output: Transformierte Daten
        """
    
    def reverse(data: dict) -> dict:
        """
        Spielt Animation rückwärts ab
        
        Action: Für jedes animierte Property:
                - Keyframes zeitlich umkehren: t_new = op - t_old
                - Keyframe-Reihenfolge umkehren
        Output: Transformierte Daten
        """
    
    # ===== ADVANCED TRANSFORMATIONS (Mittel) =====
    
    def recolor(data: dict, old_color: list, new_color: list, 
                tolerance: float = 0.1) -> dict:
        """
        Ersetzt Farbe
        
        Input:  old_color = [r, g, b, a] (0-1)
                new_color = [r, g, b, a]
                tolerance = Farbabweichung akzeptiert (0-1)
        Action: Findet alle Fill-Shapes mit old_color
                Ersetzt durch new_color
        Output: Transformierte Daten
        """
    
    def isolate_layers(data: dict, layer_indices: list) -> dict:
        """
        Behält nur bestimmte Layers
        
        Input:  layer_indices = [1, 3, 5]
        Action: Löscht alle anderen Layers
                Behält Parent-Child-Beziehungen intakt
        Output: Transformierte Daten (nur isolierte Layers)
        """
    
    def delete_layers(data: dict, layer_indices: list) -> dict:
        """
        Löscht bestimmte Layers
        
        Input:  layer_indices = [2, 4]
        Action: Entfernt Layers aus data["layers"]
                Aktualisiert Parent-Referenzen
        Output: Transformierte Daten
        """
    
    def combine(data1: dict, data2: dict, mode: str = "overlay") -> dict:
        """
        Kombiniert zwei Animationen
        
        Input:  data1, data2 (zwei Lottie-Animationen)
                mode = "overlay" (übereinander) oder "sequence" (nacheinander)
        Action: overlay: Merged layers von beiden
                sequence: data1 Frames 0-N, data2 Frames N+1 - M
        Output: Neue kombinierte Animation
        """
    
    def extract_frames(data: dict, start: int, end: int) -> dict:
        """
        Extrahiert Frame-Bereich
        
        Input:  start_frame, end_frame
        Action: Schneidet Animation zu
                Passt alle Keyframes an (nur Bereich start-end)
        Output: Gekürzte Animation
        """
```

### Technische Sicherheit:
- ✅ **Immer Kopie erstellen** (Original bleibt unberührt)
- ✅ **Validierung vor/nach** Transformation
- ✅ **Try-Except** für fehlertoleranz

---

## 🧠 MODUL 4: BUILDER (Intelligente Übungs-Erstellung)

### Zweck:
Hochlevel-Funktionen für Übungs-Erstellung (nutzt Transformer intern)

### Funktionen:

```python
class LottieBuilder:
    
    def build_exercise(target: str, source_file: str = None, 
                      strategy: str = "auto") -> dict:
        """
        Erstellt Übung automatisch
        
        Input:  target = "Box Jumps"
                source_file = "Burpees.json" (optional)
                strategy = "auto" (automatisch wählen) oder manuell
        
        Action: 1. Wenn source_file=None: find_best_source(target)
                2. load_source(source_file)
                3. apply_strategy(target, strategy)
                4. return transformed_data
        
        Output: Fertige Übungs-Animation
        """
    
    def find_best_source(target_exercise: str, top_n: int = 3) -> list:
        """
        Findet beste Basis-Animation
        
        Input:  target_exercise = "Box Jumps"
        Action: Analysiert alle 44 Animationen
                Vergleicht mit target (Bewegungstyp, Ähnlichkeit)
                Rankt nach Erfolgswahrscheinlichkeit
        Output: [
            {"file": "Burpees.json", "score": 0.95, "reason": "Hat Jump-Phase"},
            {"file": "Kniebeugen.json", "score": 0.60, "reason": "Bein-Bewegung"},
            ...
        ]
        """
    
    def suggest_transformations(source: str, target: str) -> list:
        """
        Schlägt Transformationen vor
        
        Input:  source = "Burpees.json", target = "Box Jumps"
        Output: [
            {"action": "extract_frames", "params": {"start": 141, "end": 164}, "reason": "Jump-Phase"},
            {"action": "speed", "params": {"multiplier": 1.2}, "reason": "Schneller springen"},
            {"action": "mirror", "params": {"axis": "horizontal"}, "reason": "Optional für Varianz"}
        ]
        """
    
    def batch_build(targets: list, output_dir: str) -> dict:
        """
        Erstellt mehrere Übungen
        
        Input:  targets = ["Box Jumps", "Wandsitzen", "Step-ups"]
                output_dir = "D:\\...\\animations\\"
        Action: Für jede Übung: build_exercise()
                Speichert Ergebnis in output_dir
        Output: {
            "Box Jumps": {"success": True, "file": "box-jumps.json"},
            "Wandsitzen": {"success": True, "file": "wandsitzen.json"},
            ...
        }
        """
```

### Strategie-Datenbank (für build_exercise):

```python
EXERCISE_STRATEGIES = {
    "Box Jumps": {
        "source": "Burpees.json",
        "steps": [
            {"action": "extract_frames", "params": {"start": 141, "end": 164}},
            {"action": "speed", "params": {"multiplier": 1.1}}
        ]
    },
    "Wandsitzen": {
        "source": "Kniebeugen.json",
        "steps": [
            {"action": "extract_frames", "params": {"start": 60, "end": 120}},
            {"action": "speed", "params": {"multiplier": 0.1}}  # Fast statisch
        ]
    },
    # ... für alle 8 machbaren Übungen
}
```

---

## 💻 MODUL 5: CLI INTERFACE (Bedienung)

### Zweck:
Kommandozeilen-Interface (für mich scriptable bedienbar)

### Befehle:

```bash
# ===== BASIC TRANSFORMATIONS =====

python lottie_builder.py mirror <input.json> <output.json> [--axis horizontal|vertical]
python lottie_builder.py rotate <input.json> <output.json> --degrees 90
python lottie_builder.py scale <input.json> <output.json> --factor 1.5
python lottie_builder.py speed <input.json> <output.json> --multiplier 2.0
python lottie_builder.py reverse <input.json> <output.json>

# ===== ADVANCED TRANSFORMATIONS =====

python lottie_builder.py recolor <input.json> <output.json> --old "0.5,0.2,0.8" --new "0.9,0.6,0.4"
python lottie_builder.py isolate <input.json> <output.json> --layers 1,3,5
python lottie_builder.py delete <input.json> <output.json> --layers 2,4
python lottie_builder.py combine <input1.json> <input2.json> <output.json> [--mode overlay|sequence]
python lottie_builder.py extract <input.json> <output.json> --start 60 --end 120

# ===== BUILDER (HIGH-LEVEL) =====

python lottie_builder.py build "Box Jumps" --output box-jumps.json [--source Burpees.json]
python lottie_builder.py find-source "Box Jumps" [--top 3]
python lottie_builder.py suggest "Burpees.json" "Box Jumps"
python lottie_builder.py batch "Box Jumps,Wandsitzen,Step-ups" --output-dir "D:\\...\\animations"

# ===== ANALYZER (INFO) =====

python lottie_builder.py analyze <input.json> [--type structure|animation|colors]
python lottie_builder.py info <input.json>
python lottie_builder.py preview <input.json> [--output preview.gif]

# ===== UTILITY =====

python lottie_builder.py validate <input.json>
python lottie_builder.py list <verzeichnis>  # Listet alle Lotties
```

### Ausgabe-Format (für mich lesbar):

```
✅ SUCCESS: Animation transformiert
   Input:  Burpees.json (1100x1300, 7.2s, 20 layers)
   Action: extract_frames(141-164)
   Output: box-jumps.json (1100x1300, 1.0s, 20 layers)
   File:   D:\Entwicklung\Android\FORMIX\app\src\main\assets\animations\box-jumps.json
```

```
❌ ERROR: Transformation fehlgeschlagen
   Input:  Liegestütz.json
   Action: mirror(horizontal)
   Error:  Invalid layer structure after transformation
   Fix:    Versuche mit --force Flag
```

---

## 📁 DATEI-STRUKTUR

```
C:\Users\kim\lottie_builder\
│
├── lottie_builder.py          # Haupt-CLI-Datei
├── core.py                     # MODUL 1: Core-Funktionen
├── analyzer.py                 # MODUL 2: Analyzer
├── transformer.py              # MODUL 3: Transformer
├── builder.py                  # MODUL 4: Builder
├── strategies.py               # Strategie-Datenbank für 14 Übungen
├── utils.py                    # Helper-Funktionen
└── README.md                   # Dokumentation
```

---

## 🔄 WORKFLOW-BEISPIEL

### Beispiel: "Box Jumps" erstellen

```bash
# Schritt 1: Beste Quelle finden
python lottie_builder.py find-source "Box Jumps"

# Output:
# 1. Burpees.json (Score: 0.95) - Hat Jump-Phase
# 2. Kniebeugen.json (Score: 0.60) - Bein-Bewegung

# Schritt 2: Transformations-Vorschläge
python lottie_builder.py suggest "Burpees.json" "Box Jumps"

# Output:
# Suggested transformations:
#   1. extract_frames(141, 164) - Jump-Phase isolieren
#   2. speed(1.1) - Schneller springen

# Schritt 3: Automatisch erstellen
python lottie_builder.py build "Box Jumps" --output box-jumps.json

# Output:
# ✅ SUCCESS: box-jumps.json erstellt
#    Quelle: Burpees.json
#    Transformationen: extract_frames, speed
#    Qualität: 90%

# Schritt 4: Manuell testen
python lottie_builder.py preview box-jumps.json --output box-jumps-preview.gif

# Schritt 5: In FORMIX einbauen
# (Manuelle Aktion: LottieAnimationProvider.kt erweitern)
```

---

## ⚙️ TECHNISCHE ANFORDERUNGEN

### Python-Pakete (installiert ✅):
- `json` (Standard Library)
- `pathlib` (Standard Library)
- `copy` (Standard Library)
- `argparse` (Standard Library)
- `re` (Standard Library)

### Optional (für Preview):
- `pillow` (schon installiert ✅)
- `lottie` (schon installiert ✅)

### Entwicklungs-Umgebung:
- ✅ Python 3.12.10
- ✅ Windows 11
- ✅ Wezterm (UTF-8 kompatibel)
- ✅ FORMIX-Projekt bei `D:\Entwicklung\Android\FORMIX`

---

## 🎯 QUALITÄTSSICHERUNG

### Jede Transformation muss:
1. ✅ **Validierung VORHER** - Input ist gültiges Lottie JSON
2. ✅ **Kopie erstellen** - Original bleibt unberührt
3. ✅ **Transformation durchführen** - Mit Try-Except
4. ✅ **Validierung NACHHER** - Output ist gültiges Lottie JSON
5. ✅ **Backup erstellen** - Original als .bak speichern
6. ✅ **Logging** - Was wurde gemacht, Erfolg/Fehler

### Test-Strategie:
```python
def test_transformation(transform_func, input_file):
    # 1. Lade Original
    original = load_json(input_file)
    
    # 2. Validiere Original
    assert validate_lottie(original)[0], "Original invalid"
    
    # 3. Transformiere
    result = transform_func(original)
    
    # 4. Validiere Ergebnis
    assert validate_lottie(result)[0], "Result invalid"
    
    # 5. Vergleiche Metadata
    assert result["v"] == original["v"], "Version changed"
    assert len(result["layers"]) >= 1, "No layers"
    
    # 6. Return
    return True
```

---

## ✅ ZUSAMMENFASSUNG TEIL 2

### Was wir geplant haben:
1. ✅ **5 Module:** Core, Analyzer, Transformer, Builder, CLI
2. ✅ **9 Basic/Advanced Transformationen:** mirror, rotate, scale, speed, reverse, recolor, isolate, combine, extract
3. ✅ **4 Builder-Funktionen:** build_exercise, find_best_source, suggest_transformations, batch_build
4. ✅ **CLI-Interface:** Scriptable, lesbare Ausgabe, alle Funktionen erreichbar
5. ✅ **Qualitätssicherung:** Validierung, Backup, Testing

### Architektur-Prinzipien:
- 🧩 **Modular** - Jedes Modul eigenständig testbar
- 🔧 **Scriptable** - Ich kann es per CLI bedienen
- 🛡️ **Sicher** - Keine Daten verloren (Backups, Validierung)
- 📈 **Erweiterbar** - Neue Transformationen einfach hinzufügbar

### Nächster Schritt:
**TEIL 3: Basic Transformations Spezifikation** - Detaillierte technische Specs für mirror, rotate, scale, speed, reverse

---

**Status:** ✅ TEIL 2 ABGESCHLOSSEN  
**Bereit für:** TEIL 3 Planung
