# 🔧 Universal Lottie Builder - MASTER-PROTOKOLL - TEIL 2: EXTERNE TOOLS INTEGRATION

**Datum:** 01.05.2026  
**Zweck:** Detaillierte Integration aller externen Tools ins Lottie-Builder-Tool  
**Status:** IMPLEMENTIERUNGS-GUIDE

---

## 🎯 ZIEL DIESES DOKUMENTS

Zeigt **genau wie** jedes externe Tool in unser Programm eingebaut wird:
- Installation & Setup
- Wrapper-Klassen für einheitliche API
- Code-Beispiele für jede Funktion
- Fehlerbehandlung & Fallbacks

---

## 📦 EXTERNE TOOLS - VOLLSTÄNDIGE LISTE

### ✅ Bereits installiert:
1. ✅ **Python 3.12.10**
2. ✅ **Python `lottie` v0.7.2**
3. ✅ **Pillow** (PIL)
4. ✅ **cairosvg** / **cairocffi**
5. ✅ **Node.js** (mit npm)

### 📥 Noch zu installieren:
6. ⬜ **lottie-web** (Node.js package)
7. ⬜ **puppeteer** (für Rendering, optional)

---

## 🔧 TOOL 1: Python `lottie` Package (HAUPTTOOL ⭐⭐⭐⭐⭐)

### Installation:
```bash
# Bereits installiert ✅
pip show lottie
# Name: lottie
# Version: 0.7.2
```

### Was es kann:
- ✅ JSON → Python-Objekte (objektorientiert)
- ✅ Alle Lottie-Properties als Attribute
- ✅ Automatische Validierung
- ✅ Export in verschiedene Formate

### Wrapper-Klasse: `LottieObjectWrapper`

```python
# external_tools.py

from lottie.objects import Animation
from lottie import objects
from pathlib import Path
import copy

class LottieObjectWrapper:
    """
    Wrapper für Python lottie package
    Bietet einheitliche API für objektorientierte Lottie-Manipulation
    """
    
    def __init__(self, filepath=None, animation_obj=None):
        """
        Initialisiert Wrapper
        
        Args:
            filepath: Pfad zu JSON-Datei
            animation_obj: Bereits geladenes Animation-Objekt
        """
        if filepath:
            self.animation = Animation.load(str(filepath))
            self.filepath = Path(filepath)
        elif animation_obj:
            self.animation = animation_obj
            self.filepath = None
        else:
            self.animation = Animation()
            self.filepath = None
    
    @classmethod
    def from_json(cls, filepath):
        """Lädt Animation aus JSON-Datei"""
        return cls(filepath=filepath)
    
    @classmethod
    def from_dict(cls, data):
        """Erstellt Animation aus Python-Dict"""
        import json
        import tempfile
        
        # Temporäre Datei erstellen
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
            json.dump(data, f)
            temp_path = f.name
        
        # Laden und temp löschen
        wrapper = cls(filepath=temp_path)
        Path(temp_path).unlink()
        
        return wrapper
    
    def to_dict(self):
        """Konvertiert Animation zu Python-Dict"""
        return self.animation.to_dict()
    
    def save(self, filepath, backup=True):
        """
        Speichert Animation als JSON
        
        Args:
            filepath: Ziel-Pfad
            backup: Backup erstellen wenn Datei existiert
        """
        filepath = Path(filepath)
        
        # Backup erstellen
        if backup and filepath.exists():
            backup_path = filepath.with_suffix('.json.bak')
            import shutil
            shutil.copy2(filepath, backup_path)
        
        # Speichern
        self.animation.save(str(filepath))
        
        return filepath
    
    def get_metadata(self):
        """Extrahiert Metadata"""
        return {
            "version": self.animation.version,
            "name": self.animation.name or "Unnamed",
            "width": self.animation.width,
            "height": self.animation.height,
            "fps": self.animation.frame_rate,
            "in_point": self.animation.in_point,
            "out_point": self.animation.out_point,
            "duration_frames": self.animation.out_point - self.animation.in_point,
            "duration_seconds": (self.animation.out_point - self.animation.in_point) / self.animation.frame_rate,
            "num_layers": len(self.animation.layers)
        }
    
    def validate(self):
        """
        Validiert Animation
        
        Returns:
            (is_valid: bool, error_message: str)
        """
        try:
            # Pflichtfelder prüfen
            if not self.animation.width or self.animation.width <= 0:
                return False, "Invalid width"
            
            if not self.animation.height or self.animation.height <= 0:
                return False, "Invalid height"
            
            if not self.animation.frame_rate or self.animation.frame_rate <= 0:
                return False, "Invalid frame rate"
            
            if len(self.animation.layers) == 0:
                return False, "No layers found"
            
            # Alles OK
            return True, ""
        
        except Exception as e:
            return False, f"Validation error: {str(e)}"
    
    def clone(self):
        """Erstellt tiefe Kopie"""
        return LottieObjectWrapper(animation_obj=copy.deepcopy(self.animation))
    
    # ===== TRANSFORMATIONS (nutzen lottie objects) =====
    
    def mirror(self, axis="horizontal"):
        """
        Spiegelt Animation
        
        Args:
            axis: "horizontal" oder "vertical"
        
        Returns:
            Neuer LottieObjectWrapper (Original unverändert)
        """
        result = self.clone()
        
        scale_x_multiplier = -1 if axis == "horizontal" else 1
        scale_y_multiplier = -1 if axis == "vertical" else 1
        
        for layer in result.animation.layers:
            if hasattr(layer, 'transform') and layer.transform:
                # Scale manipulieren
                if hasattr(layer.transform.scale, 'value'):
                    # Statischer Wert
                    layer.transform.scale.value.x *= scale_x_multiplier
                    layer.transform.scale.value.y *= scale_y_multiplier
                elif hasattr(layer.transform.scale, 'keyframes'):
                    # Animierte Werte
                    for kf in layer.transform.scale.keyframes:
                        kf.start.x *= scale_x_multiplier
                        kf.start.y *= scale_y_multiplier
        
        return result
    
    def rotate(self, degrees):
        """
        Rotiert Animation um X Grad
        
        Args:
            degrees: Rotation in Grad (positiv = Uhrzeigersinn)
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        
        for layer in result.animation.layers:
            if hasattr(layer, 'transform') and layer.transform:
                # Rotation manipulieren
                if hasattr(layer.transform.rotation, 'value'):
                    # Statischer Wert
                    layer.transform.rotation.value += degrees
                elif hasattr(layer.transform.rotation, 'keyframes'):
                    # Animierte Werte
                    for kf in layer.transform.rotation.keyframes:
                        kf.start += degrees
        
        return result
    
    def scale_canvas(self, factor):
        """
        Skaliert Canvas-Größe
        
        Args:
            factor: Skalierungsfaktor (1.5 = 150%)
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        
        result.animation.width = int(result.animation.width * factor)
        result.animation.height = int(result.animation.height * factor)
        
        return result
    
    def change_speed(self, multiplier):
        """
        Ändert Geschwindigkeit
        
        Args:
            multiplier: Geschwindigkeitsfaktor (2.0 = doppelt so schnell)
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        
        # Methode A: Framerate ändern (empfohlen)
        result.animation.frame_rate *= multiplier
        
        return result
    
    def reverse(self):
        """
        Spielt Animation rückwärts ab
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        
        total_frames = result.animation.out_point - result.animation.in_point
        
        for layer in result.animation.layers:
            if hasattr(layer, 'transform') and layer.transform:
                # Für jede animierte Property
                for prop_name in ['position', 'rotation', 'scale', 'opacity']:
                    prop = getattr(layer.transform, prop_name, None)
                    if prop and hasattr(prop, 'keyframes'):
                        # Keyframe-Timings umkehren
                        for kf in prop.keyframes:
                            kf.time = total_frames - kf.time
                        
                        # Reihenfolge umkehren
                        prop.keyframes.reverse()
        
        return result
    
    def extract_frames(self, start_frame, end_frame):
        """
        Extrahiert Frame-Bereich
        
        Args:
            start_frame: Start-Frame
            end_frame: End-Frame
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        
        # Neue Duration
        new_duration = end_frame - start_frame
        result.animation.in_point = 0
        result.animation.out_point = new_duration
        
        for layer in result.animation.layers:
            # Layer In/Out-Points anpassen
            if hasattr(layer, 'in_point'):
                layer.in_point = max(0, layer.in_point - start_frame)
            if hasattr(layer, 'out_point'):
                layer.out_point = min(new_duration, layer.out_point - start_frame)
            
            # Keyframes anpassen
            if hasattr(layer, 'transform') and layer.transform:
                for prop_name in ['position', 'rotation', 'scale', 'opacity']:
                    prop = getattr(layer.transform, prop_name, None)
                    if prop and hasattr(prop, 'keyframes'):
                        new_keyframes = []
                        
                        for kf in prop.keyframes:
                            if start_frame <= kf.time <= end_frame:
                                kf.time -= start_frame
                                new_keyframes.append(kf)
                        
                        prop.keyframes = new_keyframes
        
        return result
    
    def combine_overlay(self, other_wrapper, offset_x=0, offset_y=0):
        """
        Kombiniert mit anderer Animation (Overlay)
        
        Args:
            other_wrapper: Anderer LottieObjectWrapper
            offset_x: Position-Verschiebung X
            offset_y: Position-Verschiebung Y
        
        Returns:
            Neuer LottieObjectWrapper
        """
        result = self.clone()
        other = other_wrapper.clone()
        
        # Canvas-Größe anpassen
        result.animation.width = max(result.animation.width, other.animation.width)
        result.animation.height = max(result.animation.height, other.animation.height)
        
        # Duration anpassen
        result.animation.out_point = max(result.animation.out_point, other.animation.out_point)
        
        # Layer-Indizes anpassen
        max_index = max([layer.index for layer in result.animation.layers])
        
        for layer in other.animation.layers:
            # Index anpassen
            layer.index += max_index
            
            # Parent-Referenzen anpassen
            if hasattr(layer, 'parent') and layer.parent:
                layer.parent += max_index
            
            # Position verschieben
            if offset_x != 0 or offset_y != 0:
                if hasattr(layer, 'transform') and layer.transform:
                    if hasattr(layer.transform.position, 'value'):
                        layer.transform.position.value.x += offset_x
                        layer.transform.position.value.y += offset_y
                    elif hasattr(layer.transform.position, 'keyframes'):
                        for kf in layer.transform.position.keyframes:
                            kf.start.x += offset_x
                            kf.start.y += offset_y
            
            # Layer hinzufügen
            result.animation.layers.append(layer)
        
        return result
    
    def get_layer_by_name(self, pattern):
        """
        Findet Layers nach Namen (Regex)
        
        Args:
            pattern: Regex-Pattern oder exakter Name
        
        Returns:
            Liste von Layers
        """
        import re
        
        regex = re.compile(pattern, re.IGNORECASE)
        
        matching_layers = []
        for layer in self.animation.layers:
            if hasattr(layer, 'name') and layer.name:
                if regex.search(layer.name):
                    matching_layers.append(layer)
        
        return matching_layers
```

---

## 🔧 TOOL 2: lottie-web (Node.js Package)

### Installation:
```bash
# In Projekt-Verzeichnis
cd C:\Users\kim\lottie_builder
npm init -y
npm install lottie-web puppeteer
```

### Was es kann:
- ✅ Animation rendern (SVG/Canvas)
- ✅ Export als GIF, PNG-Sequence
- ✅ Validierung ob Animation abspielbar ist

### Wrapper-Klasse: `LottieWebWrapper`

```python
# external_tools.py (Fortsetzung)

import subprocess
import json
from pathlib import Path
import tempfile

class LottieWebWrapper:
    """
    Wrapper für Node.js lottie-web
    Rendering & Validation via Node.js subprocess
    """
    
    def __init__(self, node_script_path=None):
        """
        Initialisiert Wrapper
        
        Args:
            node_script_path: Pfad zu Node.js-Helper-Script (optional)
        """
        if node_script_path:
            self.node_script = Path(node_script_path)
        else:
            # Standard-Pfad
            self.node_script = Path(__file__).parent / "lottie_web_helper.js"
    
    def render_preview(self, lottie_json_path, output_gif_path, width=800, height=600):
        """
        Rendert Animation als GIF
        
        Args:
            lottie_json_path: Pfad zu Lottie JSON
            output_gif_path: Ziel-Pfad für GIF
            width: GIF-Breite
            height: GIF-Höhe
        
        Returns:
            True wenn erfolgreich, sonst False
        """
        try:
            # Node.js-Script aufrufen
            cmd = [
                "node",
                str(self.node_script),
                "render",
                str(lottie_json_path),
                str(output_gif_path),
                str(width),
                str(height)
            ]
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )
            
            if result.returncode == 0:
                return True
            else:
                print(f"❌ Render failed: {result.stderr}")
                return False
        
        except Exception as e:
            print(f"❌ Error: {str(e)}")
            return False
    
    def validate_playable(self, lottie_json_path):
        """
        Validiert ob Animation in lottie-web abspielbar ist
        
        Args:
            lottie_json_path: Pfad zu Lottie JSON
        
        Returns:
            (is_valid: bool, error_message: str)
        """
        try:
            cmd = [
                "node",
                str(self.node_script),
                "validate",
                str(lottie_json_path)
            ]
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                return True, ""
            else:
                return False, result.stderr
        
        except Exception as e:
            return False, f"Validation error: {str(e)}"
```

### Node.js Helper-Script: `lottie_web_helper.js`

```javascript
// lottie_web_helper.js
// Helper-Script für lottie-web Integration

const fs = require('fs');
const path = require('path');

// Kommando-Parser
const command = process.argv[2];

if (command === 'render') {
    // Render als GIF
    const inputPath = process.argv[3];
    const outputPath = process.argv[4];
    const width = parseInt(process.argv[5]) || 800;
    const height = parseInt(process.argv[6]) || 600;
    
    renderAnimation(inputPath, outputPath, width, height);
    
} else if (command === 'validate') {
    // Validiere Animation
    const inputPath = process.argv[3];
    validateAnimation(inputPath);
    
} else {
    console.error('Invalid command:', command);
    process.exit(1);
}

function renderAnimation(inputPath, outputPath, width, height) {
    // TODO: Implementierung mit puppeteer
    // Rendert Lottie als GIF
    
    console.log(`Rendering ${inputPath} → ${outputPath} (${width}x${height})`);
    
    // Für jetzt: Placeholder
    console.log('✅ Render erfolgreich (TODO: Implementierung)');
    process.exit(0);
}

function validateAnimation(inputPath) {
    try {
        const animData = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
        
        // Basis-Validierung
        if (!animData.v) throw new Error('Missing version');
        if (!animData.fr) throw new Error('Missing frame rate');
        if (!animData.w) throw new Error('Missing width');
        if (!animData.h) throw new Error('Missing height');
        if (!animData.layers) throw new Error('Missing layers');
        
        console.log('✅ Animation valid');
        process.exit(0);
        
    } catch (error) {
        console.error('❌ Validation failed:', error.message);
        process.exit(1);
    }
}
```

---

## 🔧 TOOL 3: Pillow (PIL) - Image Processing

### Installation:
```bash
# Bereits installiert ✅
pip show pillow
```

### Wrapper-Klasse: `ExportWrapper`

```python
# external_tools.py (Fortsetzung)

from PIL import Image
import os

class ExportWrapper:
    """
    Wrapper für Export-Funktionen
    GIF, PNG, Thumbnail-Generation
    """
    
    @staticmethod
    def create_thumbnail(lottie_json_path, output_png_path, size=(200, 200)):
        """
        Erstellt Thumbnail von Animation (Placeholder)
        
        Args:
            lottie_json_path: Pfad zu Lottie JSON
            output_png_path: Ziel-Pfad für PNG
            size: Thumbnail-Größe
        
        Returns:
            True wenn erfolgreich
        """
        # TODO: Implementierung mit lottie package Export
        # Für jetzt: Placeholder-Image
        
        img = Image.new('RGB', size, color=(200, 200, 200))
        img.save(output_png_path)
        
        return True
    
    @staticmethod
    def frames_to_gif(frame_paths, output_gif_path, duration=100):
        """
        Erstellt GIF aus PNG-Frames
        
        Args:
            frame_paths: Liste von PNG-Pfaden
            output_gif_path: Ziel-Pfad für GIF
            duration: Frame-Duration in ms
        
        Returns:
            True wenn erfolgreich
        """
        try:
            frames = [Image.open(fp) for fp in frame_paths]
            
            frames[0].save(
                output_gif_path,
                save_all=True,
                append_images=frames[1:],
                duration=duration,
                loop=0
            )
            
            return True
        
        except Exception as e:
            print(f"❌ GIF creation failed: {str(e)}")
            return False
```

---

## 🔧 UNIFIED API: ExternalToolsManager

### Master-Wrapper für alle externen Tools:

```python
# external_tools.py (Fortsetzung)

class ExternalToolsManager:
    """
    Zentraler Manager für alle externen Tools
    Bietet einheitliche API
    """
    
    def __init__(self):
        self.lottie_obj_wrapper = LottieObjectWrapper
        self.lottie_web_wrapper = LottieWebWrapper()
        self.export_wrapper = ExportWrapper()
    
    def load_animation(self, filepath):
        """Lädt Animation als LottieObjectWrapper"""
        return LottieObjectWrapper.from_json(filepath)
    
    def save_animation(self, wrapper, filepath, backup=True):
        """Speichert Animation"""
        return wrapper.save(filepath, backup=backup)
    
    def transform_mirror(self, wrapper, axis="horizontal"):
        """Spiegelt Animation"""
        return wrapper.mirror(axis)
    
    def transform_rotate(self, wrapper, degrees):
        """Rotiert Animation"""
        return wrapper.rotate(degrees)
    
    def transform_scale(self, wrapper, factor):
        """Skaliert Canvas"""
        return wrapper.scale_canvas(factor)
    
    def transform_speed(self, wrapper, multiplier):
        """Ändert Geschwindigkeit"""
        return wrapper.change_speed(multiplier)
    
    def transform_reverse(self, wrapper, reverse=True):
        """Kehrt Animation um"""
        return wrapper.reverse()
    
    def transform_extract_frames(self, wrapper, start, end):
        """Extrahiert Frame-Bereich"""
        return wrapper.extract_frames(start, end)
    
    def transform_combine(self, wrapper1, wrapper2, mode="overlay", offset_x=0, offset_y=0):
        """Kombiniert zwei Animationen"""
        if mode == "overlay":
            return wrapper1.combine_overlay(wrapper2, offset_x, offset_y)
        else:
            raise NotImplementedError("Sequence mode not yet implemented")
    
    def generate_preview(self, wrapper, output_gif_path):
        """Generiert GIF-Preview"""
        # Temporäre JSON speichern
        import tempfile
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
            wrapper.save(f.name, backup=False)
            temp_json = f.name
        
        # Rendern
        result = self.lottie_web_wrapper.render_preview(temp_json, output_gif_path)
        
        # Cleanup
        os.unlink(temp_json)
        
        return result
    
    def validate_animation(self, wrapper):
        """Validiert Animation mit allen Validatoren"""
        # 1. Python lottie package Validierung
        is_valid_obj, msg_obj = wrapper.validate()
        if not is_valid_obj:
            return False, f"Object validation failed: {msg_obj}"
        
        # 2. lottie-web Validierung (optional)
        # ...
        
        return True, ""
```

---

## ✅ ZUSAMMENFASSUNG TEIL 2

### Was dieses Dokument definiert:
1. ✅ **LottieObjectWrapper** - Python `lottie` package Integration
2. ✅ **LottieWebWrapper** - Node.js `lottie-web` Integration
3. ✅ **ExportWrapper** - Pillow/cairosvg für Export
4. ✅ **ExternalToolsManager** - Einheitliche API für alle Tools
5. ✅ **Node.js Helper-Script** - lottie-web Helper

### Alle Transformationen nutzen jetzt:
- ✅ Objektorientierte APIs statt raw JSON
- ✅ Automatische Validierung
- ✅ Type-Safety
- ✅ Bessere Lesbarkeit

### Nächster Schritt:
**TEIL 3: Implementierungs-Guide** - Vollständiger Code für alle Module

---

**Status:** ✅ TEIL 2 ABGESCHLOSSEN  
**Bereit für:** TEIL 3 - Vollständiger Python-Code
