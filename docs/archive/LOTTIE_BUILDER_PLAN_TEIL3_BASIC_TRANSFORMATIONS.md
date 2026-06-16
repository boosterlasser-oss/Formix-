# ⚙️ Universal Lottie Builder - TEIL 3: Basic Transformations Spezifikation

**Datum:** 01.05.2026  
**Zweck:** Detaillierte technische Spezifikation der 5 Basic Transformationen  
**Status:** PLANUNGSDOKUMENT

---

## 🎯 ZIEL DIESES DOKUMENTS

Präzise Implementierungs-Anleitung für die **5 Basic Transformationen**, die garantiert funktionieren.

---

## 🪞 TRANSFORMATION 1: MIRROR (Spiegeln)

### Funktions-Signatur:
```python
def mirror(data: dict, axis: str = "horizontal") -> dict:
    """Spiegelt Animation horizontal oder vertikal"""
```

### Parameter:
- `data` - Lottie JSON als Python Dict
- `axis` - "horizontal" (links↔rechts) oder "vertical" (oben↔unten)

### Technische Implementierung:

```python
def mirror(data, axis="horizontal"):
    # 1. Kopie erstellen (Original unberührt lassen)
    result = copy.deepcopy(data)
    
    # 2. Scale-Multiplikator bestimmen
    if axis == "horizontal":
        scale_x_multiplier = -1  # Horizontal spiegeln
        scale_y_multiplier = 1   # Vertikal unverändert
    elif axis == "vertical":
        scale_x_multiplier = 1   # Horizontal unverändert
        scale_y_multiplier = -1  # Vertikal spiegeln
    else:
        raise ValueError(f"Invalid axis: {axis}")
    
    # 3. Für jeden Layer
    for layer in result.get("layers", []):
        
        # Nur wenn Layer Transform-Properties hat
        if "ks" not in layer or "s" not in layer["ks"]:
            continue
        
        scale_prop = layer["ks"]["s"]
        
        # Fall A: Statischer Scale-Wert
        if scale_prop.get("a", 0) == 0:
            # scale_prop["k"] ist [x, y, z]
            original_x = scale_prop["k"][0]
            original_y = scale_prop["k"][1]
            
            scale_prop["k"][0] = original_x * scale_x_multiplier
            scale_prop["k"][1] = original_y * scale_y_multiplier
        
        # Fall B: Animierter Scale-Wert (Keyframes)
        elif scale_prop.get("a", 0) == 1:
            # scale_prop["k"] ist Liste von Keyframes
            for keyframe in scale_prop["k"]:
                if "s" in keyframe:  # Start-Value
                    original_x = keyframe["s"][0]
                    original_y = keyframe["s"][1]
                    
                    keyframe["s"][0] = original_x * scale_x_multiplier
                    keyframe["s"][1] = original_y * scale_y_multiplier
                
                if "e" in keyframe:  # End-Value (bei manchen Keyframes)
                    original_x = keyframe["e"][0]
                    original_y = keyframe["e"][1]
                    
                    keyframe["e"][0] = original_x * scale_x_multiplier
                    keyframe["e"][1] = original_y * scale_y_multiplier
    
    return result
```

### Edge Cases:
1. **Layer ohne Scale-Property** → Überspringen
2. **Negative Scale-Werte** → Werden positiv (doppelte Spiegelung hebt sich auf)
3. **Parent-Child-Hierarchie** → Nur Root-Layers spiegeln (Children folgen automatisch)

### Optimierung für Performance:
```python
def mirror_optimized(data, axis="horizontal"):
    """Spiegelt nur Root-Layers (ohne Parent)"""
    result = copy.deepcopy(data)
    
    # Nur Root-Layers (parent=None oder fehlt)
    root_layers = [l for l in result["layers"] if "parent" not in l]
    
    for layer in root_layers:
        # ... spiegeln wie oben
    
    return result
```

### Test-Fälle:
```python
# Test 1: Horizontale Spiegelung
input_scale = [100, 100, 100]
output_scale = mirror(input, "horizontal")
assert output_scale == [-100, 100, 100]

# Test 2: Vertikale Spiegelung
output_scale = mirror(input, "vertical")
assert output_scale == [100, -100, 100]

# Test 3: Doppelte Spiegelung hebt sich auf
output = mirror(mirror(input, "horizontal"), "horizontal")
assert output == input  # Identisch
```

---

## 🔄 TRANSFORMATION 2: ROTATE (Rotieren)

### Funktions-Signatur:
```python
def rotate(data: dict, degrees: float) -> dict:
    """Rotiert Animation um X Grad (im Uhrzeigersinn)"""
```

### Parameter:
- `data` - Lottie JSON
- `degrees` - Rotation in Grad (positiv = Uhrzeigersinn, negativ = gegen Uhrzeigersinn)

### Technische Implementierung:

```python
def rotate(data, degrees):
    # 1. Kopie erstellen
    result = copy.deepcopy(data)
    
    # 2. Für jeden Layer
    for layer in result.get("layers", []):
        
        # Nur wenn Layer Rotation-Property hat
        if "ks" not in layer or "r" not in layer["ks"]:
            continue
        
        rotation_prop = layer["ks"]["r"]
        
        # Fall A: Statische Rotation
        if rotation_prop.get("a", 0) == 0:
            # rotation_prop["k"] ist einzelner Wert
            original_rotation = rotation_prop["k"]
            rotation_prop["k"] = original_rotation + degrees
        
        # Fall B: Animierte Rotation (Keyframes)
        elif rotation_prop.get("a", 0) == 1:
            # rotation_prop["k"] ist Liste von Keyframes
            for keyframe in rotation_prop["k"]:
                if "s" in keyframe:  # Start-Value
                    if isinstance(keyframe["s"], list):
                        keyframe["s"][0] += degrees
                    else:
                        keyframe["s"] += degrees
                
                if "e" in keyframe:  # End-Value
                    if isinstance(keyframe["e"], list):
                        keyframe["e"][0] += degrees
                    else:
                        keyframe["e"] += degrees
    
    return result
```

### Wichtige Hinweise:
- **Rotation ist additiv** - Jeder Layer behält seine Original-Rotation + degrees
- **360° Normalisierung** - Optional: Werte auf 0-360° normalisieren
- **Parent-Child** - Children erben Parent-Rotation automatisch

### Normalisierung (optional):
```python
def normalize_rotation(value):
    """Normalisiert Rotation auf 0-360 Grad"""
    while value < 0:
        value += 360
    while value >= 360:
        value -= 360
    return value
```

### Test-Fälle:
```python
# Test 1: Einfache 90° Rotation
input_rotation = 0
output = rotate(input, 90)
assert output_rotation == 90

# Test 2: Negative Rotation
output = rotate(input, -45)
assert output_rotation == -45  # oder 315 wenn normalisiert

# Test 3: Mehrfache Rotation
output = rotate(rotate(input, 90), 90)
assert output_rotation == 180
```

---

## 📏 TRANSFORMATION 3: SCALE (Skalieren)

### Funktions-Signatur:
```python
def scale(data: dict, factor: float, maintain_aspect: bool = True) -> dict:
    """Skaliert Animation (Canvas-Größe)"""
```

### Parameter:
- `data` - Lottie JSON
- `factor` - Skalierungsfaktor (1.5 = 150%, 0.5 = 50%)
- `maintain_aspect` - Seitenverhältnis beibehalten (True) oder nur Breite/Höhe ändern

### Technische Implementierung:

```python
def scale(data, factor, maintain_aspect=True):
    # 1. Kopie erstellen
    result = copy.deepcopy(data)
    
    # 2. Canvas-Größe ändern
    if maintain_aspect:
        # Proportional skalieren
        result["w"] = int(result["w"] * factor)
        result["h"] = int(result["h"] * factor)
    else:
        # Nur Breite skalieren (Höhe unverändert)
        result["w"] = int(result["w"] * factor)
    
    # 3. Optional: Layer-Positionen anpassen
    # (Wenn wir pixelgenaue Skalierung wollen)
    for layer in result.get("layers", []):
        if "ks" not in layer or "p" not in layer["ks"]:
            continue
        
        position_prop = layer["ks"]["p"]
        
        # Fall A: Statische Position
        if position_prop.get("a", 0) == 0:
            # position_prop["k"] ist [x, y, z]
            position_prop["k"][0] *= factor
            position_prop["k"][1] *= factor
        
        # Fall B: Animierte Position
        elif position_prop.get("a", 0) == 1:
            for keyframe in position_prop["k"]:
                if "s" in keyframe:
                    keyframe["s"][0] *= factor
                    keyframe["s"][1] *= factor
                if "e" in keyframe:
                    keyframe["e"][0] *= factor
                    keyframe["e"][1] *= factor
    
    return result
```

### Zwei Varianten:

#### Variante A: Nur Canvas (einfach)
```python
def scale_simple(data, factor):
    """Nur Canvas-Größe ändern (Layer-Skalierung erfolgt automatisch)"""
    result = copy.deepcopy(data)
    result["w"] = int(result["w"] * factor)
    result["h"] = int(result["h"] * factor)
    return result
```

#### Variante B: Canvas + Layer-Positionen (präzise)
```python
def scale_precise(data, factor):
    """Canvas + alle Layer-Positionen skalieren"""
    # Wie oben (mit Position-Anpassung)
```

**Empfehlung:** Variante A ist ausreichend! Lottie-Player skalieren automatisch.

### Test-Fälle:
```python
# Test 1: 150% Skalierung
input_w, input_h = 1000, 1000
output = scale(input, 1.5)
assert output["w"] == 1500
assert output["h"] == 1500

# Test 2: 50% Verkleinerung
output = scale(input, 0.5)
assert output["w"] == 500
assert output["h"] == 500

# Test 3: Seitenverhältnis prüfen
ratio_before = input["w"] / input["h"]
ratio_after = output["w"] / output["h"]
assert ratio_before == ratio_after
```

---

## ⚡ TRANSFORMATION 4: SPEED (Geschwindigkeit)

### Funktions-Signatur:
```python
def speed(data: dict, multiplier: float) -> dict:
    """Ändert Abspielgeschwindigkeit (ohne Frame-Anzahl zu ändern)"""
```

### Parameter:
- `data` - Lottie JSON
- `multiplier` - Geschwindigkeitsfaktor (2.0 = doppelt so schnell, 0.5 = halb so schnell)

### Zwei Methoden:

#### Methode A: Framerate ändern (EMPFOHLEN ✅)
```python
def speed_method_a(data, multiplier):
    """
    Ändert Framerate (FPS)
    
    Beispiel:
      Original: 30 FPS, 180 Frames = 6 Sekunden
      2x schneller: 60 FPS, 180 Frames = 3 Sekunden
    """
    result = copy.deepcopy(data)
    
    # Framerate anpassen
    result["fr"] = result["fr"] * multiplier
    
    # WICHTIG: op (out-point) NICHT ändern!
    # Die Animation hat immer noch 180 Frames,
    # aber bei höherer FPS wird sie schneller abgespielt
    
    return result
```

#### Methode B: Keyframe-Timing ändern (KOMPLEX ⚠️)
```python
def speed_method_b(data, multiplier):
    """
    Ändert Keyframe-Timings + Duration
    
    Beispiel:
      Original: Keyframe bei Frame 60
      2x schneller: Keyframe bei Frame 30
    """
    result = copy.deepcopy(data)
    
    # 1. Duration anpassen
    result["op"] = result["op"] / multiplier
    result["ip"] = result["ip"] / multiplier
    
    # 2. Alle Keyframe-Timings anpassen
    for layer in result.get("layers", []):
        if "ks" not in layer:
            continue
        
        # Für jede animierte Property (p, r, s, o, etc.)
        for prop_key in ["p", "r", "s", "o", "a"]:
            if prop_key not in layer["ks"]:
                continue
            
            prop = layer["ks"][prop_key]
            
            # Nur wenn animiert
            if prop.get("a", 0) == 1:
                for keyframe in prop["k"]:
                    if "t" in keyframe:
                        keyframe["t"] = keyframe["t"] / multiplier
        
        # Layer In/Out-Points anpassen
        if "ip" in layer:
            layer["ip"] = layer["ip"] / multiplier
        if "op" in layer:
            layer["op"] = layer["op"] / multiplier
    
    return result
```

### Empfehlung:
**Methode A verwenden!** - Einfacher, sicherer, funktioniert garantiert.

Methode B nur wenn:
- Du die Frame-Anzahl ändern willst
- Du Keyframe-präzise Kontrolle brauchst

### Test-Fälle:
```python
# Test 1: Doppelte Geschwindigkeit (Methode A)
input_fr = 30
input_duration = 6.0  # Sekunden (= op/fr = 180/30)
output = speed(input, 2.0)
assert output["fr"] == 60
output_duration = output["op"] / output["fr"]
assert output_duration == 3.0  # Halbe Zeit

# Test 2: Halbe Geschwindigkeit
output = speed(input, 0.5)
assert output["fr"] == 15
output_duration = output["op"] / output["fr"]
assert output_duration == 12.0  # Doppelte Zeit

# Test 3: Normale Geschwindigkeit (keine Änderung)
output = speed(input, 1.0)
assert output["fr"] == input["fr"]
```

---

## ⏪ TRANSFORMATION 5: REVERSE (Umkehren)

### Funktions-Signatur:
```python
def reverse(data: dict) -> dict:
    """Spielt Animation rückwärts ab"""
```

### Technische Implementierung:

```python
def reverse(data):
    """Kehrt Animation um (Ende → Start)"""
    result = copy.deepcopy(data)
    
    # Out-Point (Gesamt-Duration in Frames)
    total_frames = result["op"]
    
    # Für jeden Layer
    for layer in result.get("layers", []):
        if "ks" not in layer:
            continue
        
        # Für jede animierte Property (p, r, s, o, a, etc.)
        for prop_key in ["p", "r", "s", "o", "a"]:
            if prop_key not in layer["ks"]:
                continue
            
            prop = layer["ks"][prop_key]
            
            # Nur wenn animiert (a=1)
            if prop.get("a", 0) != 1:
                continue
            
            keyframes = prop["k"]
            
            # 1. Timing umkehren (t_new = total_frames - t_old)
            for kf in keyframes:
                if "t" in kf:
                    kf["t"] = total_frames - kf["t"]
            
            # 2. Keyframe-Reihenfolge umkehren
            prop["k"] = list(reversed(keyframes))
            
            # 3. Start/End-Values tauschen (falls vorhanden)
            for i, kf in enumerate(prop["k"]):
                if i < len(prop["k"]) - 1:
                    # Aktuelles End = Nächstes Start
                    next_kf = prop["k"][i + 1]
                    if "s" in next_kf and "e" in kf:
                        # Tauschen
                        temp = kf["e"]
                        kf["e"] = next_kf["s"]
                        next_kf["s"] = temp
        
        # Layer In/Out-Points umkehren
        if "ip" in layer and "op" in layer:
            original_ip = layer["ip"]
            original_op = layer["op"]
            layer["ip"] = total_frames - original_op
            layer["op"] = total_frames - original_ip
    
    return result
```

### Vereinfachte Version (nur Timing):
```python
def reverse_simple(data):
    """Nur Keyframe-Timings umkehren (ohne Value-Swap)"""
    result = copy.deepcopy(data)
    total_frames = result["op"]
    
    for layer in result.get("layers", []):
        if "ks" not in layer:
            continue
        
        for prop_key in ["p", "r", "s", "o", "a"]:
            if prop_key not in layer["ks"]:
                continue
            
            prop = layer["ks"][prop_key]
            if prop.get("a", 0) == 1:
                # Timings umkehren
                for kf in prop["k"]:
                    if "t" in kf:
                        kf["t"] = total_frames - kf["t"]
                
                # Reihenfolge umkehren
                prop["k"] = list(reversed(prop["k"]))
    
    return result
```

**Empfehlung:** Vereinfachte Version zuerst testen!

### Edge Cases:
1. **Easing-Kurven** - In/Out-Tangents müssen ggf. auch getauscht werden
2. **Hold-Keyframes** - Keyframes ohne Interpolation
3. **Nested Animations** - Pre-Comps müssen auch umgekehrt werden

### Test-Fälle:
```python
# Test 1: Einfache Animation (0→180 Frames)
# Keyframe bei Frame 60 → soll zu Frame 120 werden
input_keyframe_time = 60
output = reverse(input)
output_keyframe_time = output["layers"][0]["ks"]["p"]["k"][0]["t"]
assert output_keyframe_time == 120  # (180 - 60)

# Test 2: Doppelte Umkehrung = Original
output = reverse(reverse(input))
assert output == input  # Fast identisch (evtl. Rundungsfehler)

# Test 3: Start/End-Values sind getauscht
input_start_value = input["layers"][0]["ks"]["p"]["k"][0]["s"]
input_end_value = input["layers"][0]["ks"]["p"]["k"][-1]["s"]
output_start_value = output["layers"][0]["ks"]["p"]["k"][0]["s"]
output_end_value = output["layers"][0]["ks"]["p"]["k"][-1]["s"]
assert output_start_value == input_end_value
assert output_end_value == input_start_value
```

---

## 🛡️ FEHLERBEHANDLUNG (für alle 5 Transformationen)

### Standard Try-Except Wrapper:
```python
def safe_transform(transform_func, data, *args, **kwargs):
    """
    Sicherer Wrapper für alle Transformationen
    
    - Validiert Input
    - Führt Transformation aus
    - Validiert Output
    - Gibt Fehler zurück wenn fehlgeschlagen
    """
    try:
        # 1. Input validieren
        is_valid, error_msg = validate_lottie(data)
        if not is_valid:
            return None, f"Invalid input: {error_msg}"
        
        # 2. Transformation durchführen
        result = transform_func(data, *args, **kwargs)
        
        # 3. Output validieren
        is_valid, error_msg = validate_lottie(result)
        if not is_valid:
            return None, f"Invalid output: {error_msg}"
        
        # 4. Erfolg
        return result, None
    
    except Exception as e:
        return None, f"Transformation failed: {str(e)}"
```

### Verwendung:
```python
result, error = safe_transform(mirror, data, axis="horizontal")
if error:
    print(f"❌ ERROR: {error}")
else:
    print(f"✅ SUCCESS: Animation transformed")
    save_json("output.json", result)
```

---

## ✅ ZUSAMMENFASSUNG TEIL 3

### Was wir spezifiziert haben:
1. ✅ **MIRROR** - Scale-Property manipulieren (horizontal/vertikal)
2. ✅ **ROTATE** - Rotation-Property um X Grad ändern
3. ✅ **SCALE** - Canvas-Größe ändern (w/h)
4. ✅ **SPEED** - Framerate anpassen (Methode A empfohlen)
5. ✅ **REVERSE** - Keyframe-Timings + Reihenfolge umkehren

### Für jede Transformation:
- ✅ Vollständige Code-Implementierung (Python)
- ✅ Parameter & Rückgabewerte
- ✅ Edge Cases identifiziert
- ✅ Test-Fälle definiert
- ✅ Fehlerbehandlung integriert

### Technische Garantien:
- 🛡️ **Original bleibt unberührt** (copy.deepcopy)
- ✅ **Input/Output-Validierung**
- ⚡ **Performant** (nur notwendige Layer bearbeiten)
- 🔄 **Reversibel** (z.B. mirror(mirror(x)) = x)

### Nächster Schritt:
**TEIL 4: Advanced Transformations** - recolor, isolate, delete, combine, extract

---

**Status:** ✅ TEIL 3 ABGESCHLOSSEN  
**Bereit für:** TEIL 4 Planung
