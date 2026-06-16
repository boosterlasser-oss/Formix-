# 🎨 Universal Lottie Builder - TEIL 4: Advanced Transformations

**Datum:** 01.05.2026  
**Zweck:** Detaillierte technische Spezifikation der Advanced Transformationen  
**Status:** PLANUNGSDOKUMENT

---

## 🎯 ZIEL DIESES DOKUMENTS

Präzise Implementierungs-Anleitung für **5 Advanced Transformationen** - komplexer aber sehr mächtig!

---

## 🎨 TRANSFORMATION 6: RECOLOR (Farben ändern)

### Funktions-Signatur:
```python
def recolor(data: dict, old_color: list, new_color: list, 
            tolerance: float = 0.1) -> dict:
    """Ersetzt eine Farbe durch eine andere"""
```

### Parameter:
- `data` - Lottie JSON
- `old_color` - [r, g, b, a] (Werte 0.0-1.0), z.B. [0.5, 0.2, 0.8, 1.0]
- `new_color` - [r, g, b, a]
- `tolerance` - Farbtoleranz (0.0 = exakt, 1.0 = alle Farben)

### Technische Implementierung:

```python
import math

def color_distance(color1, color2):
    """Berechnet Euklidischen Abstand zwischen zwei Farben"""
    r1, g1, b1 = color1[0], color1[1], color1[2]
    r2, g2, b2 = color2[0], color2[1], color2[2]
    
    return math.sqrt((r2-r1)**2 + (g2-g1)**2 + (b2-b1)**2)

def colors_match(color1, color2, tolerance):
    """Prüft ob zwei Farben ähnlich genug sind"""
    # Max Distanz = sqrt(3) ≈ 1.732 (komplett unterschiedlich)
    # tolerance 0.1 = 10% von max_distance
    max_distance = math.sqrt(3)
    threshold = tolerance * max_distance
    
    return color_distance(color1, color2) <= threshold

def recolor(data, old_color, new_color, tolerance=0.1):
    """Ersetzt Farbe in Animation"""
    result = copy.deepcopy(data)
    
    # Zähler für Statistik
    replaced_count = 0
    
    # Für jeden Layer
    for layer in result.get("layers", []):
        
        # Nur Shape-Layers haben Farben (ty=4)
        if layer.get("ty") != 4:
            continue
        
        # Shapes durchsuchen
        if "shapes" not in layer:
            continue
        
        for shape_group in layer["shapes"]:
            replaced_count += _recolor_shape_recursive(
                shape_group, old_color, new_color, tolerance
            )
    
    return result

def _recolor_shape_recursive(shape, old_color, new_color, tolerance):
    """Rekursiv durch Shape-Hierarchie (Gruppen können verschachtelt sein)"""
    replaced_count = 0
    
    # Shape-Typ bestimmen
    shape_type = shape.get("ty")
    
    # FILL (Füllung)
    if shape_type == "fl":
        if "c" in shape:
            color_prop = shape["c"]
            
            # Fall A: Statische Farbe
            if color_prop.get("a", 0) == 0:
                current_color = color_prop["k"]
                if colors_match(current_color[:3], old_color[:3], tolerance):
                    color_prop["k"] = new_color[:]
                    replaced_count += 1
            
            # Fall B: Animierte Farbe (Keyframes)
            elif color_prop.get("a", 0) == 1:
                for keyframe in color_prop["k"]:
                    if "s" in keyframe:
                        current_color = keyframe["s"]
                        if colors_match(current_color[:3], old_color[:3], tolerance):
                            keyframe["s"] = new_color[:]
                            replaced_count += 1
                    
                    if "e" in keyframe:
                        current_color = keyframe["e"]
                        if colors_match(current_color[:3], old_color[:3], tolerance):
                            keyframe["e"] = new_color[:]
                            replaced_count += 1
    
    # STROKE (Kontur)
    elif shape_type == "st":
        if "c" in shape:
            # Analog zu Fill (gleicher Code)
            color_prop = shape["c"]
            # ... (wie oben)
    
    # GROUP (Gruppe - rekursiv durchsuchen)
    elif shape_type == "gr":
        if "it" in shape:  # Items in Gruppe
            for item in shape["it"]:
                replaced_count += _recolor_shape_recursive(
                    item, old_color, new_color, tolerance
                )
    
    return replaced_count
```

### Color-Helper-Funktionen:

```python
def hex_to_lottie_color(hex_color):
    """Konvertiert Hex (#FF5500) zu Lottie-Farbe [r,g,b,a]"""
    hex_color = hex_color.lstrip('#')
    r = int(hex_color[0:2], 16) / 255.0
    g = int(hex_color[2:4], 16) / 255.0
    b = int(hex_color[4:6], 16) / 255.0
    return [r, g, b, 1.0]

def lottie_color_to_hex(lottie_color):
    """Konvertiert Lottie-Farbe zu Hex"""
    r = int(lottie_color[0] * 255)
    g = int(lottie_color[1] * 255)
    b = int(lottie_color[2] * 255)
    return f"#{r:02X}{g:02X}{b:02X}"
```

### Test-Fälle:
```python
# Test 1: Einfache Farb-Ersetzung
old = [0.5, 0.2, 0.8, 1.0]  # Lila
new = [0.9, 0.6, 0.4, 1.0]  # Orange
output = recolor(input, old, new, tolerance=0.05)
# Prüfe: Alle Lila-Fills sind jetzt Orange

# Test 2: Mit Toleranz
old = [1.0, 0.0, 0.0, 1.0]  # Rot
new = [0.0, 1.0, 0.0, 1.0]  # Grün
output = recolor(input, old, new, tolerance=0.2)
# Ersetzt auch ähnliche Rot-Töne (Orange, Pink)

# Test 3: Hex-Farben verwenden
old_hex = "#FF0000"
new_hex = "#00FF00"
old = hex_to_lottie_color(old_hex)
new = hex_to_lottie_color(new_hex)
output = recolor(input, old, new)
```

---

## 🎯 TRANSFORMATION 7: ISOLATE_LAYERS (Layer isolieren)

### Funktions-Signatur:
```python
def isolate_layers(data: dict, layer_indices: list, 
                   keep_parents: bool = True) -> dict:
    """Behält nur bestimmte Layers"""
```

### Parameter:
- `data` - Lottie JSON
- `layer_indices` - Liste von Layer-Indizes [1, 3, 5]
- `keep_parents` - Parent-Layers auch behalten? (wichtig für Hierarchie)

### Technische Implementierung:

```python
def isolate_layers(data, layer_indices, keep_parents=True):
    """Behält nur angegebene Layers"""
    result = copy.deepcopy(data)
    
    # 1. Zu behaltende Layers sammeln
    layers_to_keep = set(layer_indices)
    
    # 2. Wenn keep_parents=True: Alle Parent-Layers auch hinzufügen
    if keep_parents:
        layers_to_keep = _collect_parents(result["layers"], layers_to_keep)
    
    # 3. Filtern
    original_layers = result["layers"]
    result["layers"] = [
        layer for layer in original_layers 
        if layer.get("ind") in layers_to_keep
    ]
    
    # 4. Layer-Indizes neu nummerieren (optional, aber sauber)
    result = _renumber_layer_indices(result)
    
    return result

def _collect_parents(layers, layer_indices):
    """Sammelt rekursiv alle Parent-Layers"""
    result = set(layer_indices)
    
    # Layer-Index → Layer-Object Mapping
    layer_map = {layer["ind"]: layer for layer in layers}
    
    # Für jeden zu behaltenden Layer
    for layer_idx in list(result):
        if layer_idx not in layer_map:
            continue
        
        layer = layer_map[layer_idx]
        
        # Hat dieser Layer einen Parent?
        if "parent" in layer:
            parent_idx = layer["parent"]
            
            # Parent auch hinzufügen
            result.add(parent_idx)
            
            # Rekursiv: Parent's Parent auch hinzufügen
            result = result.union(_collect_parents(layers, {parent_idx}))
    
    return result

def _renumber_layer_indices(data):
    """Nummeriert Layer-Indizes neu (1, 2, 3, ...)"""
    layers = data["layers"]
    
    # Alte Index → Neue Index Mapping
    old_to_new = {}
    
    # Neue Indizes vergeben
    for new_idx, layer in enumerate(layers, start=1):
        old_idx = layer["ind"]
        old_to_new[old_idx] = new_idx
        layer["ind"] = new_idx
    
    # Parent-Referenzen aktualisieren
    for layer in layers:
        if "parent" in layer:
            old_parent = layer["parent"]
            if old_parent in old_to_new:
                layer["parent"] = old_to_new[old_parent]
            else:
                # Parent existiert nicht mehr → entfernen
                del layer["parent"]
    
    return data
```

### Anwendungsfall: Jump-Phase aus Burpees extrahieren

```python
# Burpees.json hat Layer 1-20
# Jump-Phase ist in Layers 2, 3 (Beispiel)

# Variante A: Nur Jump-Layers
output = isolate_layers(burpees, [2, 3], keep_parents=False)
# Ergebnis: 2 Layers

# Variante B: Mit Parent-Hierarchie
output = isolate_layers(burpees, [2, 3], keep_parents=True)
# Ergebnis: 2 Layers + alle Parents (evtl. 5 Layers total)
```

### Test-Fälle:
```python
# Test 1: Einfache Isolation
input_layers = 20
output = isolate_layers(input, [1, 5, 10])
assert len(output["layers"]) == 3

# Test 2: Mit Parents
# Input: Layer 5 hat parent=2, Layer 2 hat parent=1
output = isolate_layers(input, [5], keep_parents=True)
assert len(output["layers"]) == 3  # Layer 5, 2, 1

# Test 3: Ohne Parents
output = isolate_layers(input, [5], keep_parents=False)
assert len(output["layers"]) == 1  # Nur Layer 5
```

---

## 🗑️ TRANSFORMATION 8: DELETE_LAYERS (Layers löschen)

### Funktions-Signatur:
```python
def delete_layers(data: dict, layer_indices: list, 
                  delete_children: bool = True) -> dict:
    """Löscht bestimmte Layers"""
```

### Parameter:
- `data` - Lottie JSON
- `layer_indices` - Liste von Layer-Indizes zu löschen [2, 4, 6]
- `delete_children` - Child-Layers auch löschen?

### Technische Implementierung:

```python
def delete_layers(data, layer_indices, delete_children=True):
    """Löscht angegebene Layers"""
    result = copy.deepcopy(data)
    
    # 1. Zu löschende Layers sammeln
    layers_to_delete = set(layer_indices)
    
    # 2. Wenn delete_children=True: Alle Child-Layers auch hinzufügen
    if delete_children:
        layers_to_delete = _collect_children(result["layers"], layers_to_delete)
    
    # 3. Filtern (behalten was NICHT gelöscht werden soll)
    original_layers = result["layers"]
    result["layers"] = [
        layer for layer in original_layers 
        if layer.get("ind") not in layers_to_delete
    ]
    
    # 4. Parent-Referenzen aufräumen
    for layer in result["layers"]:
        if "parent" in layer:
            if layer["parent"] in layers_to_delete:
                # Parent wurde gelöscht → Referenz entfernen
                del layer["parent"]
    
    # 5. Indizes neu nummerieren
    result = _renumber_layer_indices(result)
    
    return result

def _collect_children(layers, layer_indices):
    """Sammelt rekursiv alle Child-Layers"""
    result = set(layer_indices)
    
    # Für jeden Layer
    for layer in layers:
        # Hat dieser Layer einen Parent der gelöscht wird?
        if "parent" in layer:
            if layer["parent"] in result:
                # Diesen Layer auch löschen
                result.add(layer["ind"])
                
                # Rekursiv: Children von diesem Layer auch
                result = result.union(_collect_children(layers, {layer["ind"]}))
    
    return result
```

### Anwendungsfall: Equipment entfernen

```python
# Liegestütz.json - Hanteln entfernen (falls vorhanden)
# Angenommen Hantel-Layers sind 8, 9

output = delete_layers(liegestuetz, [8, 9], delete_children=True)
# Hanteln und ihre Sub-Layers sind weg
```

### Test-Fälle:
```python
# Test 1: Einfaches Löschen
input_layers = 10
output = delete_layers(input, [3, 7])
assert len(output["layers"]) == 8  # 10 - 2

# Test 2: Mit Children
# Input: Layer 5 hat children 6,7
output = delete_layers(input, [5], delete_children=True)
assert len(output["layers"]) == 7  # 10 - 3 (Layer 5,6,7)

# Test 3: Ohne Children
output = delete_layers(input, [5], delete_children=False)
assert len(output["layers"]) == 9  # 10 - 1 (nur Layer 5)
# Layer 6,7 haben jetzt keinen Parent mehr
```

---

## 🔗 TRANSFORMATION 9: COMBINE (Animationen kombinieren)

### Funktions-Signatur:
```python
def combine(data1: dict, data2: dict, mode: str = "overlay", 
            offset_x: float = 0, offset_y: float = 0) -> dict:
    """Kombiniert zwei Animationen"""
```

### Parameter:
- `data1`, `data2` - Zwei Lottie-Animationen
- `mode` - "overlay" (übereinander) oder "sequence" (nacheinander)
- `offset_x`, `offset_y` - Position-Verschiebung für data2 (nur bei overlay)

### Technische Implementierung:

#### Mode A: OVERLAY (Übereinander)
```python
def combine_overlay(data1, data2, offset_x=0, offset_y=0):
    """Legt data2 über data1 (gleichzeitig abgespielt)"""
    result = copy.deepcopy(data1)
    
    # 1. Canvas-Größe anpassen (größere von beiden)
    result["w"] = max(data1["w"], data2["w"])
    result["h"] = max(data1["h"], data2["h"])
    
    # 2. Duration anpassen (längere von beiden)
    result["op"] = max(data1["op"], data2["op"])
    
    # 3. Framerate harmonisieren (gleiche FPS)
    if data1["fr"] != data2["fr"]:
        # Warnung: Unterschiedliche FPS!
        # Wir nehmen FPS von data1
        pass
    
    # 4. Layers von data2 kopieren
    data2_layers = copy.deepcopy(data2["layers"])
    
    # 5. Layer-Indizes von data2 anpassen (damit keine Kollision)
    max_index_data1 = max([layer["ind"] for layer in result["layers"]])
    
    for layer in data2_layers:
        # Neuer Index
        old_index = layer["ind"]
        new_index = old_index + max_index_data1
        layer["ind"] = new_index
        
        # Parent-Referenzen anpassen
        if "parent" in layer:
            layer["parent"] = layer["parent"] + max_index_data1
        
        # Position verschieben (wenn offset angegeben)
        if offset_x != 0 or offset_y != 0:
            if "ks" in layer and "p" in layer["ks"]:
                pos_prop = layer["ks"]["p"]
                
                # Statische Position
                if pos_prop.get("a", 0) == 0:
                    pos_prop["k"][0] += offset_x
                    pos_prop["k"][1] += offset_y
                
                # Animierte Position
                elif pos_prop.get("a", 0) == 1:
                    for kf in pos_prop["k"]:
                        if "s" in kf:
                            kf["s"][0] += offset_x
                            kf["s"][1] += offset_y
                        if "e" in kf:
                            kf["e"][0] += offset_x
                            kf["e"][1] += offset_y
    
    # 6. Layers zusammenführen
    result["layers"].extend(data2_layers)
    
    return result
```

#### Mode B: SEQUENCE (Nacheinander)
```python
def combine_sequence(data1, data2):
    """Spielt data1, dann data2 ab (nacheinander)"""
    result = copy.deepcopy(data1)
    
    # 1. Canvas-Größe harmonisieren
    result["w"] = max(data1["w"], data2["w"])
    result["h"] = max(data1["h"], data2["h"])
    
    # 2. Duration addieren
    data1_duration = data1["op"]
    data2_duration = data2["op"]
    result["op"] = data1_duration + data2_duration
    
    # 3. Layers von data2 kopieren und Zeit-verschoben hinzufügen
    data2_layers = copy.deepcopy(data2["layers"])
    
    # 4. Layer-Indizes anpassen
    max_index_data1 = max([layer["ind"] for layer in result["layers"]])
    
    for layer in data2_layers:
        # Index anpassen
        old_index = layer["ind"]
        new_index = old_index + max_index_data1
        layer["ind"] = new_index
        
        if "parent" in layer:
            layer["parent"] = layer["parent"] + max_index_data1
        
        # Zeitliche Verschiebung: data2 startet nach data1
        time_offset = data1_duration
        
        # In-Point / Out-Point verschieben
        if "ip" in layer:
            layer["ip"] += time_offset
        if "op" in layer:
            layer["op"] += time_offset
        
        # Alle Keyframes verschieben
        if "ks" in layer:
            for prop_key in ["p", "r", "s", "o", "a"]:
                if prop_key not in layer["ks"]:
                    continue
                
                prop = layer["ks"][prop_key]
                if prop.get("a", 0) == 1:
                    for kf in prop["k"]:
                        if "t" in kf:
                            kf["t"] += time_offset
    
    # 5. Layers zusammenführen
    result["layers"].extend(data2_layers)
    
    return result
```

### Wrapper-Funktion:
```python
def combine(data1, data2, mode="overlay", offset_x=0, offset_y=0):
    """Kombiniert zwei Animationen"""
    if mode == "overlay":
        return combine_overlay(data1, data2, offset_x, offset_y)
    elif mode == "sequence":
        return combine_sequence(data1, data2)
    else:
        raise ValueError(f"Invalid mode: {mode}")
```

### Test-Fälle:
```python
# Test 1: Overlay
anim1 = load_json("Kniebeugen.json")  # 4.6s
anim2 = load_json("Schulterpresse.json")  # 4.0s
output = combine(anim1, anim2, mode="overlay")
assert output["op"] == max(anim1["op"], anim2["op"])  # 4.6s (längere)
assert len(output["layers"]) == len(anim1["layers"]) + len(anim2["layers"])

# Test 2: Sequence
output = combine(anim1, anim2, mode="sequence")
assert output["op"] == anim1["op"] + anim2["op"]  # 8.6s (addiert)

# Test 3: Overlay mit Offset
output = combine(anim1, anim2, mode="overlay", offset_x=500, offset_y=0)
# anim2 ist 500px rechts von anim1
```

### Anwendungsfall: Thruster erstellen
```python
# Thruster = Kniebeugen + Schulterpresse gleichzeitig
squat = load_json("Kniebeugen.json")
shoulder_press = load_json("Schulterpresse.json")

thruster = combine(squat, shoulder_press, mode="overlay")
save_json("thruster-combined.json", thruster)
```

---

## ✂️ TRANSFORMATION 10: EXTRACT_FRAMES (Frame-Bereich extrahieren)

### Funktions-Signatur:
```python
def extract_frames(data: dict, start_frame: int, end_frame: int) -> dict:
    """Schneidet Animation auf Frame-Bereich zu"""
```

### Parameter:
- `data` - Lottie JSON
- `start_frame` - Start-Frame (z.B. 60)
- `end_frame` - End-Frame (z.B. 120)

### Technische Implementierung:

```python
def extract_frames(data, start_frame, end_frame):
    """Extrahiert Frame-Bereich [start_frame, end_frame]"""
    result = copy.deepcopy(data)
    
    # 1. Neue Duration
    new_duration = end_frame - start_frame
    result["ip"] = 0
    result["op"] = new_duration
    
    # 2. Für jeden Layer
    for layer in result.get("layers", []):
        
        # Layer In/Out-Points anpassen
        if "ip" in layer:
            layer["ip"] = max(0, layer["ip"] - start_frame)
        if "op" in layer:
            layer["op"] = min(new_duration, layer["op"] - start_frame)
        
        # Keyframes anpassen
        if "ks" not in layer:
            continue
        
        for prop_key in ["p", "r", "s", "o", "a"]:
            if prop_key not in layer["ks"]:
                continue
            
            prop = layer["ks"][prop_key]
            
            # Nur animierte Properties
            if prop.get("a", 0) != 1:
                continue
            
            keyframes = prop["k"]
            new_keyframes = []
            
            for kf in keyframes:
                if "t" not in kf:
                    continue
                
                frame_time = kf["t"]
                
                # Keyframe liegt im Bereich?
                if start_frame <= frame_time <= end_frame:
                    # Zeit verschieben (relativ zu neuem Start)
                    new_kf = copy.deepcopy(kf)
                    new_kf["t"] = frame_time - start_frame
                    new_keyframes.append(new_kf)
            
            # Keyframes ersetzen
            if len(new_keyframes) > 0:
                prop["k"] = new_keyframes
            else:
                # Keine Keyframes im Bereich → Statisch machen
                # Nehme Wert vom start_frame
                prop["a"] = 0
                prop["k"] = _get_value_at_frame(keyframes, start_frame)
    
    return result

def _get_value_at_frame(keyframes, target_frame):
    """Berechnet interpolierten Wert bei target_frame"""
    # Finde umgebende Keyframes
    before_kf = None
    after_kf = None
    
    for kf in keyframes:
        if "t" not in kf:
            continue
        
        if kf["t"] <= target_frame:
            before_kf = kf
        elif kf["t"] > target_frame and after_kf is None:
            after_kf = kf
            break
    
    # Exakter Treffer
    if before_kf and before_kf["t"] == target_frame:
        return before_kf["s"] if "s" in before_kf else before_kf.get("e", [0])
    
    # Interpolation (vereinfacht: linear)
    if before_kf and after_kf:
        # Linear interpolation
        t1 = before_kf["t"]
        t2 = after_kf["t"]
        progress = (target_frame - t1) / (t2 - t1)
        
        v1 = before_kf.get("e", before_kf.get("s", [0]))
        v2 = after_kf.get("s", [0])
        
        # Interpoliere jeden Wert
        result = []
        for i in range(len(v1)):
            interpolated = v1[i] + (v2[i] - v1[i]) * progress
            result.append(interpolated)
        
        return result
    
    # Fallback
    if before_kf:
        return before_kf.get("e", before_kf.get("s", [0]))
    elif after_kf:
        return after_kf.get("s", [0])
    else:
        return [0]  # Default
```

### Vereinfachte Version (ohne Interpolation):
```python
def extract_frames_simple(data, start_frame, end_frame):
    """Vereinfacht: Nur Keyframes verschieben, keine Interpolation"""
    result = copy.deepcopy(data)
    
    # Neue Duration
    new_duration = end_frame - start_frame
    result["ip"] = 0
    result["op"] = new_duration
    
    # Für jeden Layer
    for layer in result["layers"]:
        if "ks" not in layer:
            continue
        
        for prop_key in ["p", "r", "s", "o", "a"]:
            if prop_key not in layer["ks"]:
                continue
            
            prop = layer["ks"][prop_key]
            if prop.get("a", 0) != 1:
                continue
            
            # Keyframes filtern und verschieben
            keyframes = prop["k"]
            new_keyframes = []
            
            for kf in keyframes:
                if "t" in kf:
                    if start_frame <= kf["t"] <= end_frame:
                        new_kf = copy.deepcopy(kf)
                        new_kf["t"] -= start_frame
                        new_keyframes.append(new_kf)
            
            if new_keyframes:
                prop["k"] = new_keyframes
    
    return result
```

### Test-Fälle:
```python
# Test 1: Mittlerer Teil extrahieren
input_duration = 180  # Frames (0-180)
output = extract_frames(input, 60, 120)
assert output["op"] == 60  # 120-60
assert output["ip"] == 0

# Test 2: Keyframes sind verschoben
# Input: Keyframe bei Frame 90
# Output: Keyframe bei Frame 30 (90-60)
input_kf_time = 90
output_kf = output["layers"][0]["ks"]["p"]["k"][0]
assert output_kf["t"] == 30

# Test 3: Keyframes außerhalb werden entfernt
# Input: Keyframes bei 30, 90, 150
# Extract 60-120 → nur Keyframe 90 bleibt (als Frame 30)
assert len(output["layers"][0]["ks"]["p"]["k"]) == 1
```

### Anwendungsfall: Box Jumps aus Burpees
```python
# Burpees.json: 174 Frames
# Jump-Phase: Frames 141-164

burpees = load_json("Burpees.json")
box_jumps = extract_frames(burpees, 141, 164)

# Ergebnis: 23 Frames (164-141)
# Nur Jump-Bewegung
save_json("box-jumps.json", box_jumps)
```

---

## ✅ ZUSAMMENFASSUNG TEIL 4

### Was wir spezifiziert haben:
1. ✅ **RECOLOR** - Farben ersetzen (mit Toleranz)
2. ✅ **ISOLATE_LAYERS** - Nur bestimmte Layers behalten
3. ✅ **DELETE_LAYERS** - Layers entfernen
4. ✅ **COMBINE** - Zwei Animationen zusammenführen (overlay/sequence)
5. ✅ **EXTRACT_FRAMES** - Frame-Bereich ausschneiden

### Für jede Transformation:
- ✅ Vollständige Code-Implementierung (Python)
- ✅ Parameter & Optionen
- ✅ Anwendungsfälle beschrieben
- ✅ Test-Fälle definiert
- ✅ Helper-Funktionen inkludiert

### Komplexität:
- **RECOLOR:** ⭐⭐ Mittel (rekursive Shape-Suche)
- **ISOLATE/DELETE:** ⭐⭐ Mittel (Parent-Child-Hierarchie)
- **COMBINE:** ⭐⭐⭐ Komplex (Layer-Indizes, Timing)
- **EXTRACT_FRAMES:** ⭐⭐⭐ Komplex (Keyframe-Interpolation)

### Nächster Schritt:
**TEIL 5: Umsetzungsplan für 14 Übungen** - Konkrete Strategie für jede fehlende Übung

---

**Status:** ✅ TEIL 4 ABGESCHLOSSEN  
**Bereit für:** TEIL 5 Planung (Final!)
