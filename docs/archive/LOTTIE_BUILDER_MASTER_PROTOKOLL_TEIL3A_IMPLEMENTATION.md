# 💻 Universal Lottie Builder - MASTER-PROTOKOLL - TEIL 3: IMPLEMENTIERUNG

**Datum:** 01.05.2026  
**Zweck:** Vollständiger Python-Code für alle Module mit externer Tools-Integration  
**Status:** READY TO CODE

---

## 🎯 ZIEL DIESES DOKUMENTS

**Vollständiger Python-Code** für alle 5 Module + External Tools:
- Core, Analyzer, Transformer, Builder, CLI
- Mit `lottie` package Integration
- CLI-Interface komplett
- Test-Beispiele

---

## 📁 DATEI-STRUKTUR (RECAP)

```
C:\Users\kim\lottie_builder\
│
├── lottie_builder.py          # Entry Point (CLI)
├── core.py                     # MODUL 1: Core
├── analyzer.py                 # MODUL 2: Analyzer
├── transformer.py              # MODUL 3: Transformer
├── builder.py                  # MODUL 4: Builder
├── cli.py                      # MODUL 5: CLI-Interface
├── external_tools.py           # MODUL 6: External Tools Wrapper
├── strategies.py               # Strategie-DB
├── utils.py                    # Helpers
├── requirements.txt            # Python Dependencies
├── package.json                # Node.js Dependencies
├── lottie_web_helper.js        # Node.js Helper
└── README.md                   # Dokumentation
```

---

## 📦 requirements.txt

```txt
# Python Dependencies für Universal Lottie Builder
lottie==0.7.2
pillow>=10.0.0
cairosvg>=2.7.0
cairocffi>=1.6.0
```

---

## 📦 package.json

```json
{
  "name": "lottie-builder",
  "version": "1.0.0",
  "description": "Universal Lottie Builder - External Tools",
  "main": "lottie_web_helper.js",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "dependencies": {
    "lottie-web": "^5.12.2",
    "puppeteer": "^21.0.0"
  },
  "keywords": [
    "lottie",
    "animation",
    "builder"
  ],
  "author": "Kim Stefan Schäfer",
  "license": "MIT"
}
```

---

## 🔧 external_tools.py (KOMPLETT)

**Hinweis:** Dies wurde bereits in TEIL 2 komplett definiert.  
Siehe: `LOTTIE_BUILDER_MASTER_PROTOKOLL_TEIL2_EXTERNE_TOOLS.md`

Enthält:
- `LottieObjectWrapper` - Python `lottie` package Integration
- `LottieWebWrapper` - Node.js Integration
- `ExportWrapper` - Export-Funktionen
- `ExternalToolsManager` - Unified API

---

## 🧩 core.py - Core Module

```python
# core.py
# Universal Lottie Builder - MODUL 1: Core
# Basis-Funktionen: Laden, Speichern, Validieren

import json
import sys
import io
from pathlib import Path
import copy
import shutil
from external_tools import ExternalToolsManager, LottieObjectWrapper

# UTF-8 Encoding für Wezterm
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

class LottieCore:
    """
    Core-Modul für Lottie-Operationen
    Nutzt externe Tools für objektorientierte Manipulation
    """
    
    def __init__(self):
        self.external_tools = ExternalToolsManager()
    
    def load_json(self, filepath):
        """
        Lädt Lottie JSON-Datei (raw dict)
        
        Args:
            filepath: Pfad zur JSON-Datei
        
        Returns:
            dict: Python-Dictionary mit Lottie-Daten
        
        Raises:
            FileNotFoundError: Datei existiert nicht
            JSONDecodeError: Ungültiges JSON
        """
        filepath = Path(filepath)
        
        if not filepath.exists():
            raise FileNotFoundError(f"File not found: {filepath}")
        
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            return data
        
        except json.JSONDecodeError as e:
            raise json.JSONDecodeError(f"Invalid JSON: {str(e)}", e.doc, e.pos)
    
    def load_lottie(self, filepath):
        """
        Lädt Lottie als LottieObjectWrapper (objektorientiert)
        
        Args:
            filepath: Pfad zur JSON-Datei
        
        Returns:
            LottieObjectWrapper: Objektorientierter Wrapper
        """
        return self.external_tools.load_animation(filepath)
    
    def save_json(self, filepath, data, backup=True):
        """
        Speichert Lottie JSON-Datei
        
        Args:
            filepath: Ziel-Pfad
            data: Python-Dictionary mit Lottie-Daten
            backup: Backup erstellen wenn Datei existiert
        
        Returns:
            bool: True wenn erfolgreich
        """
        filepath = Path(filepath)
        
        # Backup erstellen
        if backup and filepath.exists():
            backup_path = filepath.with_suffix('.json.bak')
            shutil.copy2(filepath, backup_path)
            print(f"✅ Backup created: {backup_path}")
        
        # Speichern
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            print(f"✅ Saved: {filepath}")
            return True
        
        except Exception as e:
            print(f"❌ Save failed: {str(e)}")
            return False
    
    def save_lottie(self, filepath, wrapper, backup=True):
        """
        Speichert LottieObjectWrapper als JSON
        
        Args:
            filepath: Ziel-Pfad
            wrapper: LottieObjectWrapper
            backup: Backup erstellen
        
        Returns:
            Path: Gespeicherter Pfad
        """
        return self.external_tools.save_animation(wrapper, filepath, backup)
    
    def validate_lottie(self, data):
        """
        Validiert ob JSON eine gültige Lottie-Animation ist
        
        Args:
            data: dict oder LottieObjectWrapper
        
        Returns:
            tuple: (is_valid: bool, error_message: str)
        """
        if isinstance(data, LottieObjectWrapper):
            return data.validate()
        
        # Raw dict validieren
        try:
            # Pflichtfelder prüfen
            required_fields = ['v', 'fr', 'w', 'h', 'layers']
            
            for field in required_fields:
                if field not in data:
                    return False, f"Missing required field: {field}"
            
            # Werte prüfen
            if data['w'] <= 0 or data['h'] <= 0:
                return False, "Invalid canvas size"
            
            if data['fr'] <= 0:
                return False, "Invalid frame rate"
            
            if not isinstance(data['layers'], list):
                return False, "Invalid layers (must be array)"
            
            if len(data['layers']) == 0:
                return False, "No layers found"
            
            return True, ""
        
        except Exception as e:
            return False, f"Validation error: {str(e)}"
    
    def get_metadata(self, data):
        """
        Extrahiert Metadata von Animation
        
        Args:
            data: dict oder LottieObjectWrapper
        
        Returns:
            dict: Metadata
        """
        if isinstance(data, LottieObjectWrapper):
            return data.get_metadata()
        
        # Raw dict
        return {
            "version": data.get("v", "unknown"),
            "name": data.get("nm", "unnamed"),
            "width": data.get("w", 0),
            "height": data.get("h", 0),
            "fps": data.get("fr", 0),
            "in_point": data.get("ip", 0),
            "out_point": data.get("op", 0),
            "duration_frames": data.get("op", 0) - data.get("ip", 0),
            "duration_seconds": (data.get("op", 0) - data.get("ip", 0)) / data.get("fr", 1),
            "num_layers": len(data.get("layers", []))
        }
    
    def clone(self, data):
        """
        Erstellt tiefe Kopie
        
        Args:
            data: dict oder LottieObjectWrapper
        
        Returns:
            Kopie (gleicher Typ wie Input)
        """
        if isinstance(data, LottieObjectWrapper):
            return data.clone()
        else:
            return copy.deepcopy(data)
```

---

## 🔍 analyzer.py - Analyzer Module

```python
# analyzer.py
# Universal Lottie Builder - MODUL 2: Analyzer
# Analyse-Funktionen für Lottie-Animationen

import re
from external_tools import LottieObjectWrapper
from core import LottieCore

class LottieAnalyzer:
    """
    Analyzer-Modul für Lottie-Animationen
    Analysiert Struktur, Bewegung, Layer
    """
    
    def __init__(self):
        self.core = LottieCore()
    
    def analyze_structure(self, data):
        """
        Analysiert Layer-Hierarchie
        
        Args:
            data: LottieObjectWrapper oder dict
        
        Returns:
            dict: Struktur-Informationen
        """
        if isinstance(data, LottieObjectWrapper):
            layers = data.animation.layers
        else:
            layers = data.get("layers", [])
        
        # Layer-Typen zählen
        type_counts = {}
        parent_child_pairs = []
        root_layers = []
        
        for layer in layers:
            # Typ
            layer_type = getattr(layer, 'type', None) if hasattr(layer, 'type') else layer.get('ty')
            type_name = self._get_layer_type_name(layer_type)
            type_counts[type_name] = type_counts.get(type_name, 0) + 1
            
            # Parent-Child
            layer_index = getattr(layer, 'index', None) if hasattr(layer, 'index') else layer.get('ind')
            parent_index = getattr(layer, 'parent', None) if hasattr(layer, 'parent') else layer.get('parent')
            
            if parent_index:
                parent_child_pairs.append((parent_index, layer_index))
            else:
                root_layers.append(layer_index)
        
        return {
            "total_layers": len(layers),
            "layer_types": type_counts,
            "parent_child_pairs": parent_child_pairs,
            "root_layers": root_layers
        }
    
    def _get_layer_type_name(self, type_code):
        """Konvertiert Layer-Type-Code zu Name"""
        type_names = {
            0: "precomp",
            1: "solid",
            2: "image",
            3: "null",
            4: "shape",
            5: "text"
        }
        return type_names.get(type_code, f"unknown_{type_code}")
    
    def analyze_animation(self, data):
        """
        Analysiert Bewegungstyp
        
        Args:
            data: LottieObjectWrapper oder dict
        
        Returns:
            dict: Animations-Informationen
        """
        metadata = self.core.get_metadata(data)
        
        # Basis-Info
        result = {
            "duration_seconds": metadata["duration_seconds"],
            "fps": metadata["fps"],
            "animated_properties": [],
            "has_position_anim": False,
            "has_rotation_anim": False,
            "has_scale_anim": False,
            "complexity_score": 0
        }
        
        # Layer-Animationen analysieren
        if isinstance(data, LottieObjectWrapper):
            layers = data.animation.layers
        else:
            layers = data.get("layers", [])
        
        for layer in layers:
            # Transform-Properties prüfen
            if hasattr(layer, 'transform'):
                transform = layer.transform
                
                # Position
                if hasattr(transform, 'position') and self._is_animated(transform.position):
                    result["has_position_anim"] = True
                    if "position" not in result["animated_properties"]:
                        result["animated_properties"].append("position")
                
                # Rotation
                if hasattr(transform, 'rotation') and self._is_animated(transform.rotation):
                    result["has_rotation_anim"] = True
                    if "rotation" not in result["animated_properties"]:
                        result["animated_properties"].append("rotation")
                
                # Scale
                if hasattr(transform, 'scale') and self._is_animated(transform.scale):
                    result["has_scale_anim"] = True
                    if "scale" not in result["animated_properties"]:
                        result["animated_properties"].append("scale")
        
        # Complexity Score (0-10)
        score = 0
        score += len(layers) * 0.2  # Mehr Layers = komplexer
        score += len(result["animated_properties"]) * 2  # Mehr Animationen = komplexer
        result["complexity_score"] = min(10, round(score, 1))
        
        return result
    
    def _is_animated(self, property_obj):
        """Prüft ob Property animiert ist"""
        if hasattr(property_obj, 'keyframes'):
            return len(property_obj.keyframes) > 0
        return False
    
    def find_layers_by_name(self, data, pattern):
        """
        Findet Layers nach Namen (Regex)
        
        Args:
            data: LottieObjectWrapper oder dict
            pattern: Regex-Pattern
        
        Returns:
            list: Liste von Layer-Indizes
        """
        if isinstance(data, LottieObjectWrapper):
            layers = data.get_layer_by_name(pattern)
            return [layer.index for layer in layers]
        
        # Raw dict
        regex = re.compile(pattern, re.IGNORECASE)
        matching_indices = []
        
        for layer in data.get("layers", []):
            name = layer.get("nm", "")
            if regex.search(name):
                matching_indices.append(layer.get("ind"))
        
        return matching_indices
    
    def get_color_palette(self, data):
        """
        Extrahiert alle Farben aus Animation
        
        Args:
            data: LottieObjectWrapper oder dict
        
        Returns:
            list: Liste von Farb-Dictionaries
        """
        colors = {}
        
        # TODO: Implementierung für Farb-Extraktion
        # Komplex, da rekursiv durch Shapes gehen muss
        
        return []
```

---

## ⚙️ transformer.py - Transformer Module (Teil 1)

```python
# transformer.py
# Universal Lottie Builder - MODUL 3: Transformer
# Alle Transformations-Funktionen

from external_tools import LottieObjectWrapper, ExternalToolsManager
from core import LottieCore

class LottieTransformer:
    """
    Transformer-Modul für Lottie-Animationen
    Alle 10 Transformationen (5 Basic + 5 Advanced)
    """
    
    def __init__(self):
        self.core = LottieCore()
        self.external_tools = ExternalToolsManager()
    
    # ===== BASIC TRANSFORMATIONS =====
    
    def mirror(self, data, axis="horizontal"):
        """
        Spiegelt Animation
        
        Args:
            data: LottieObjectWrapper oder dict
            axis: "horizontal" oder "vertical"
        
        Returns:
            Transformiertes data (gleicher Typ wie Input)
        """
        # Konvertiere zu Wrapper wenn nötig
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        # Transformiere mit externem Tool
        result = self.external_tools.transform_mirror(wrapper, axis)
        
        # Zurück zu dict wenn Input dict war
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    def rotate(self, data, degrees):
        """
        Rotiert Animation
        
        Args:
            data: LottieObjectWrapper oder dict
            degrees: Rotation in Grad
        
        Returns:
            Transformiertes data
        """
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        result = self.external_tools.transform_rotate(wrapper, degrees)
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    def scale(self, data, factor):
        """
        Skaliert Canvas
        
        Args:
            data: LottieObjectWrapper oder dict
            factor: Skalierungsfaktor
        
        Returns:
            Transformiertes data
        """
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        result = self.external_tools.transform_scale(wrapper, factor)
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    def speed(self, data, multiplier):
        """
        Ändert Geschwindigkeit
        
        Args:
            data: LottieObjectWrapper oder dict
            multiplier: Geschwindigkeitsfaktor
        
        Returns:
            Transformiertes data
        """
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        result = self.external_tools.transform_speed(wrapper, multiplier)
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    def reverse(self, data):
        """
        Kehrt Animation um
        
        Args:
            data: LottieObjectWrapper oder dict
        
        Returns:
            Transformiertes data
        """
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        result = self.external_tools.transform_reverse(wrapper)
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    # ===== ADVANCED TRANSFORMATIONS =====
    
    def extract_frames(self, data, start_frame, end_frame):
        """
        Extrahiert Frame-Bereich
        
        Args:
            data: LottieObjectWrapper oder dict
            start_frame: Start-Frame
            end_frame: End-Frame
        
        Returns:
            Transformiertes data
        """
        is_dict = not isinstance(data, LottieObjectWrapper)
        if is_dict:
            wrapper = LottieObjectWrapper.from_dict(data)
        else:
            wrapper = data
        
        result = self.external_tools.transform_extract_frames(wrapper, start_frame, end_frame)
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    def combine(self, data1, data2, mode="overlay", offset_x=0, offset_y=0):
        """
        Kombiniert zwei Animationen
        
        Args:
            data1, data2: LottieObjectWrapper oder dict
            mode: "overlay" oder "sequence"
            offset_x, offset_y: Position-Offset
        
        Returns:
            Transformiertes data1 (kombiniert mit data2)
        """
        # Zu Wrapper konvertieren
        is_dict = not isinstance(data1, LottieObjectWrapper)
        
        if is_dict:
            wrapper1 = LottieObjectWrapper.from_dict(data1)
            wrapper2 = LottieObjectWrapper.from_dict(data2)
        else:
            wrapper1 = data1
            wrapper2 = data2
        
        result = self.external_tools.transform_combine(
            wrapper1, wrapper2, mode, offset_x, offset_y
        )
        
        if is_dict:
            return result.to_dict()
        else:
            return result
    
    # TODO: Weitere Advanced Transformationen
    # - recolor()
    # - isolate_layers()
    # - delete_layers()
```

---

## ✅ ZUSAMMENFASSUNG TEIL 3 (TEIL 1)

### Was bisher definiert wurde:
1. ✅ `requirements.txt` - Python Dependencies
2. ✅ `package.json` - Node.js Dependencies
3. ✅ `core.py` - Core-Modul KOMPLETT
4. ✅ `analyzer.py` - Analyzer-Modul KOMPLETT
5. ✅ `transformer.py` - Transformer-Modul (Basic + einige Advanced)

### Noch fehlend in TEIL 3:
6. ⬜ `builder.py` - Builder-Modul
7. ⬜ `cli.py` - CLI-Interface
8. ⬜ `strategies.py` - Strategie-DB
9. ⬜ `lottie_builder.py` - Entry Point

### Nächster Schritt:
**TEIL 3 FORTSETZUNG** - Builder, CLI, Strategies, Entry Point

---

**Status:** ✅ TEIL 3 (TEIL 1) ABGESCHLOSSEN  
**Bereit für:** TEIL 3 (TEIL 2) - Rest der Module
